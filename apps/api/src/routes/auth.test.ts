import assert from "node:assert/strict";
import test from "node:test";
import jwt from "jsonwebtoken";
import bcrypt from "bcryptjs";

const secret="test-secret";
process.env.JWT_SECRET=secret;
process.env.MONGODB_URI="mongodb://127.0.0.1:27017/test";

function reply(){return {status:0,body:null,code(n:number){this.status=n;return this},send(v:any){this.body=v;return this}}}

test("JWT ausente retorna 401",async()=>{const {requireAuth}=await import("./auth.js");const r=reply();await requireAuth({headers:{}} as any,r as any);assert.equal(r.status,401);assert.equal((r.body as any).code,"UNAUTHENTICATED")});
test("JWT inválido retorna 401",async()=>{const {requireAuth}=await import("./auth.js");const r=reply();await requireAuth({headers:{authorization:"Bearer invalido"}} as any,r as any);assert.equal(r.status,401)});
test("JWT válido identifica o usuário",async()=>{const {requireAuth}=await import("./auth.js");const token=jwt.sign({sub:"user-123",email:"u@example.com",name:"Usuário",role:"user"},secret);const req:any={headers:{authorization:"Bearer "+token}};const r=reply();await requireAuth(req,r as any);assert.equal(req.user.sub,"user-123");assert.equal(req.user.email,"u@example.com")});
test("bcrypt rejeita senha incorreta e aceita a correta",async()=>{const hash=await bcrypt.hash("senha-segura",10);assert.equal(await bcrypt.compare("errada",hash),false);assert.equal(await bcrypt.compare("senha-segura",hash),true)});
