import assert from "node:assert/strict";
import test from "node:test";
import {readFile} from "node:fs/promises";

async function source(){return readFile(new URL("../../src/routes/workbooks.ts",import.meta.url),"utf8")}

test("workbooks usam ownerId derivado do JWT",async()=>{const text=await source();assert.match(text,/ownerId:req\.user!\.sub/);assert.match(text,/normalize\(req\.body,req\.user!\.sub\)/)});
test("todas as operações de workbook exigem autenticação",async()=>{const text=await source();assert.equal((text.match(/preHandler:requireAuth/g)||[]).length,5)});
