const express = require('express');
const multer = require('multer');
const sqlite3 = require('sqlite3').verbose();
const cors = require('cors');
const fs = require('fs');
const xlsx = require('xlsx');
const csv = require('csv-parser');
const { v4: uuidv4 } = require('uuid');
const bodyParser = require('body-parser');

const app = express();
const port = 3000;

app.use(cors());
app.use(bodyParser.json({ limit: '50mb' }));

// Setup SQLite database for immutable audit log
const db = new sqlite3.Database('./audit_trail.db', (err) => {
    if (err) {
        console.error("Error opening database " + err.message);
    } else {
        db.run(`CREATE TABLE IF NOT EXISTS AuditLog (
            id TEXT PRIMARY KEY,
            timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
            eventType TEXT,
            details TEXT
        )`, (err) => {
            if (err) {
                console.error("Table creation error", err);
            }
        });
    }
});

// Helper function to log events
function logEvent(eventType, details) {
    const id = uuidv4();
    const detailsStr = typeof details === 'string' ? details : JSON.stringify(details);
    db.run(`INSERT INTO AuditLog (id, eventType, details) VALUES (?, ?, ?)`, [id, eventType, detailsStr], function(err) {
        if (err) {
            return console.error(err.message);
        }
    });
}

// Multer setup for file uploads
const upload = multer({ dest: 'uploads/' });

// File Upload Endpoint
app.post('/api/upload', upload.single('file'), (req, res) => {
    if (!req.file) {
        return res.status(400).json({ error: 'No file uploaded' });
    }

    const file = req.file;
    logEvent('FILE_UPLOAD_STARTED', { filename: file.originalname, size: file.size });

    const results = [];
    if (file.originalname.endsWith('.csv')) {
        fs.createReadStream(file.path)
            .pipe(csv())
            .on('data', (data) => results.push(data))
            .on('end', () => {
                logEvent('FILE_UPLOAD_COMPLETED', { filename: file.originalname, records: results.length });
                res.json({ message: 'CSV parsed successfully', records: results.length, preview: results.slice(0, 5) });
                // Clean up file
                fs.unlinkSync(file.path);
            });
    } else if (file.originalname.match(/\.(xls|xlsx)$/)) {
        try {
            const workbook = xlsx.readFile(file.path);
            const sheetNameList = workbook.SheetNames;
            const xlData = xlsx.utils.sheet_to_json(workbook.Sheets[sheetNameList[0]]);
            logEvent('FILE_UPLOAD_COMPLETED', { filename: file.originalname, records: xlData.length });
            res.json({ message: 'Excel parsed successfully', records: xlData.length, preview: xlData.slice(0, 5) });
            fs.unlinkSync(file.path);
        } catch (e) {
            logEvent('FILE_UPLOAD_FAILED', { filename: file.originalname, error: e.message });
            res.status(500).json({ error: 'Failed to parse Excel' });
        }
    } else {
        logEvent('FILE_UPLOAD_FAILED', { filename: file.originalname, error: 'Unsupported file type' });
        res.status(400).json({ error: 'Unsupported file type. Use .csv, .xls, or .xlsx' });
        fs.unlinkSync(file.path);
    }
});

app.get('/api/audit', (req, res) => {
    db.all("SELECT * FROM AuditLog ORDER BY timestamp DESC LIMIT 100", [], (err, rows) => {
        if (err) {
            res.status(500).json({ error: err.message });
            return;
        }
        res.json({ data: rows });
    });
});

app.listen(port, () => {
    console.log(`Mock backend listening at http://localhost:${port}`);
    logEvent('SERVER_STARTED', { port: port });
});

// Kilo Gateway Proxy - Mocking the Kilo AI endpoints
app.get('/api/gateway/models', (req, res) => {
    logEvent('MODELS_DISCOVERY_REQUEST', {});
    res.json({
        data: [
            { id: 'kilo-free-tier', capabilities: ['chat', 'analysis'], isFree: true },
            { id: 'kilo-pro', capabilities: ['chat', 'analysis', 'agents'], isFree: false },
            { id: 'gemini-1.5-pro', capabilities: ['chat', 'analysis', 'agents', 'large-context'], isFree: false }
        ]
    });
});

app.post('/api/gateway/chat/completions', (req, res) => {
    const { model, messages, stream } = req.body;

    // Security check: validate capabilities and models
    if (!model) {
        logEvent('CHAT_COMPLETION_FAILED', { error: 'No model selected' });
        return res.status(400).json({ error: 'Model selection is required' });
    }

    logEvent('CHAT_COMPLETION_REQUEST', { model: model, messageCount: messages?.length, stream: stream });

    // In a real app, this proxies to Kilo Gateway/Gemini
    // Here, we simulate the orchestration of a Coordinator and Specialist agents
    if (stream) {
        res.setHeader('Content-Type', 'text/event-stream');
        res.setHeader('Cache-Control', 'no-cache');
        res.setHeader('Connection', 'keep-alive');

        let step = 0;
        const steps = [
            JSON.stringify({ type: 'status', agent: 'Coordinator', message: 'Analyzing request...' }),
            JSON.stringify({ type: 'status', agent: 'Coordinator', message: 'Decomposing request into subtasks...' }),
            JSON.stringify({ type: 'tool', agent: 'DataSpecialist', action: 'query_dataset', arguments: { datasetId: 'active_dataset', query: 'SELECT * FROM data LIMIT 5' } }),
            JSON.stringify({ type: 'status', agent: 'DataSpecialist', message: 'Dataset queried successfully. Validating output...' }),
            JSON.stringify({ type: 'content', content: 'Based on the data, ' }),
            JSON.stringify({ type: 'content', content: 'the sales trends ' }),
            JSON.stringify({ type: 'content', content: 'show a steady increase.' }),
            JSON.stringify({ type: 'tool', agent: 'Coordinator', action: 'draft_report', arguments: { topic: 'Sales Trends' } }),
            JSON.stringify({ type: 'done' })
        ];

        const interval = setInterval(() => {
            if (step < steps.length) {
                res.write(`data: ${steps[step]}\n\n`);
                step++;
            } else {
                clearInterval(interval);
                res.end();
                logEvent('CHAT_COMPLETION_SUCCESS', { model: model, stepsSimulated: steps.length });
            }
        }, 500); // Send an event every 500ms

        req.on('close', () => {
            clearInterval(interval);
        });
    } else {
        res.json({
            choices: [
                { message: { content: 'This is a mocked response from ' + model } }
            ]
        });
        logEvent('CHAT_COMPLETION_SUCCESS', { model: model });
    }
});

// Tool invocation validation and execution endpoint (mock)
app.post('/api/tools/execute', (req, res) => {
    const { action, arguments: args, requiresApproval } = req.body;

    logEvent('TOOL_INVOCATION_REQUEST', { action, arguments: args });

    if (requiresApproval) {
        // Return 403 to trigger the approval UI on the client
        logEvent('TOOL_INVOCATION_PENDING_APPROVAL', { action });
        return res.status(403).json({
            error: 'Action requires explicit approval',
            approvalContext: {
                action: action,
                estimatedCost: '$0.05',
                affectedRecords: 'Multiple',
                destination: 'External System'
            }
        });
    }

    // Mock validation
    if (action === 'query_dataset' && !args.datasetId) {
        logEvent('TOOL_INVOCATION_FAILED', { action, error: 'datasetId is missing' });
        return res.status(400).json({ error: 'datasetId is missing' });
    }

    logEvent('TOOL_INVOCATION_SUCCESS', { action });
    res.json({ result: `Executed ${action} successfully`, data: { mockedData: true } });
});

// Action approval endpoint
app.post('/api/tools/approve', (req, res) => {
    const { actionId, approved } = req.body;

    if (approved) {
        logEvent('TOOL_ACTION_APPROVED', { actionId });
        res.json({ status: 'approved', message: 'Action executed successfully post-approval.' });
    } else {
        logEvent('TOOL_ACTION_REJECTED', { actionId });
        res.json({ status: 'rejected', message: 'Action was canceled by user.' });
    }
});
