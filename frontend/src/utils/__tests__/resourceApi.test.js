import MockAdapter from "axios-mock-adapter";
import axios from "axios";
import { uploadResource, listResources, deleteResource } from "../resourceApi";

const mock = new MockAdapter(axios);
const dummyFile = new File(["pdf"], "notes.pdf", { type: "application/pdf" });

describe("resourceApi", () => {
  beforeEach(() => mock.reset());

  test("uploadResource sends multipart/form-data", async () => {
    mock.onPost("/resources/upload").reply(200, { message: "ok" });
    const res = await uploadResource(dummyFile, "AI");
    expect(res.message).toBe("ok");
  });

  test("listResources calls /resources/list", async () => {
    mock.onGet("/resources/list").reply(200, [{ id: 1, fileName: "x.pdf" }]);
    const data = await listResources();
    expect(data[0].fileName).toBe("x.pdf");
  });

  test("deleteResource calls DELETE /resources/{id}", async () => {
    mock.onDelete("/resources/5").reply(200, { message: "deleted" });
    const res = await deleteResource(5);
    expect(res.message).toBe("deleted");
  });
});
