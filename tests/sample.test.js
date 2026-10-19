import { describe, expect, it } from "vitest";
import { TestDriver } from "testdriverai/vitest/hooks";

describe("Sample TodoMVC Test Suite", () => {
  it("should add a todo item and mark it completed", async (context) => {
    const testdriver = TestDriver(context);

    await testdriver.provision.chrome({ url: "https://todomvc.com/examples/react/dist/" });
    await testdriver.find("What needs to be done input field").click();
    await testdriver.type("Set up TestDriver CI");
    await testdriver.pressKeys(["enter"]);

    const assertResult = await testdriver.assert("The todo list contains 'Set up TestDriver CI'");
    expect(assertResult).toBeTruthy();

    await testdriver.find("checkbox toggle for Set up TestDriver CI todo item").click();

    const assertCompletedResult = await testdriver.assert("The todo item 'Set up TestDriver CI' is marked as completed");
    expect(assertCompletedResult).toBeTruthy();
  });
});
