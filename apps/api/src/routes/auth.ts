import type{FastifyInstance,FastifyReply,FastifyRequest}from"fastify";import jwt from"jsonwebtoken";import bcrypt from"bcryptjs";import{accountsDb}from"../db.js";import{config}from"../config.js";
declare module"fastify"{interface FastifyRequest{user?:{sub:string;email:string;name:string;role:string}}}
function sign(user:{id:string;email:string;name:string;role?:string}){return jwt.sign({sub:user.id,email:user.email,name:user.name,role:user.role||"user"},config.jwtSecret,{expiresIn:"7d"})}
export async function authRoutes(app:FastifyInstance){
app.post("/v1/auth/login",async(req,res)=>{
 const body=(req.body&&typeof req.body==="object"?req.body:{}) as Record<string,unknown>;
 const email=String(body.email||"").trim().toLowerCase(),password=String(body.password||"");
 if(!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)||password.length<1)return res.code(400).send({code:"INVALID_CREDENTIALS",message:"Informe email e senha."});
 const user=await accountsDb().collection("contas").findOne({"Autenticacao.Email":email});
 if(!user)return res.code(401).send({code:"INVALID_CREDENTIALS",message:"Email ou senha inválidos."});
 const hash=typeof user.Autenticacao?.SenhaHash==="string"?user.Autenticacao.SenhaHash:"";
 if(!hash||!(await bcrypt.compare(password,hash)))return res.code(401).send({code:"INVALID_CREDENTIALS",message:"Email ou senha inválidos."});
 const id=String(user.id||user._id),name=String(user.Nome||"");
 const token=sign({id,email,name,role:String(user.Conta?.Role||"user")});
 return res.send({token,user:{id,email,name,role:String(user.Conta?.Role||"user")}});
});
app.get("/v1/auth/me",{preHandler:requireAuth},async(req,res)=>res.send({user:req.user}));
}
export async function requireAuth(req:FastifyRequest,res:FastifyReply){
 const h=req.headers.authorization||"";
 if(!h.startsWith("Bearer "))return res.code(401).send({code:"UNAUTHENTICATED",message:"Login obrigatório."});
 try{const p=jwt.verify(h.slice(7),config.jwtSecret);if(typeof p!=="object"||typeof p.sub!=="string"||typeof p.email!=="string")throw new Error();req.user={sub:p.sub,email:p.email,name:typeof p.name==="string"?p.name:"",role:typeof p.role==="string"?p.role:"user"}}catch{return res.code(401).send({code:"UNAUTHENTICATED",message:"Sessão inválida ou expirada."})}
}
