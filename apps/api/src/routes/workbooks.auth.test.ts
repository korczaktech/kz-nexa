import assert from "node:assert/strict";
import test from "node:test";
import {readFile} from "node:fs/promises";

test("workbooks usam exclusivamente ownerId do JWT",async()=>{const text=await readFile(new URL("./workbooks.ts",import.meta.url),"utf8");assert.match(text,/ownerId:req\.user!\.sub/);assert.doesNotMatch(text,/req\.body[^\n]*ownerId/);assert.doesNotMatch(text,/body\.ownerId/)});
test("todas as rotas de workbook exigem autenticação",async()=>{const text=await readFile(new URL("./workbooks.ts",import.meta.url),"utf8");const routes=["/v1/workbooks","/v1/workbooks/:id"];for(const route of routes){const re=new RegExp("app\\.(get|post|put|delete)\\(\""+route.replace("/","\\/")+"[^]*?preHandler:requireAuth");assert.match(text,re)}});
