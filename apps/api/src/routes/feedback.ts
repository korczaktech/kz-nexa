import type{FastifyInstance}from"fastify";
import{productDb}from"../db.js";
import{requireAuth}from"./auth.js";

export async function feedbackRoutes(app:FastifyInstance){
 app.post("/v1/feedback",{preHandler:requireAuth},async(req,res)=>{
  const body=(req.body&&typeof req.body==="object"?req.body:{}) as Record<string,unknown>;
  const category=String(body.category||"Outro").trim();
  const subject=String(body.subject||"").trim();
  const message=String(body.message||"").trim();
  const appVersion=String(body.appVersion||"").trim();
  const platform=String(body.platform||"android").trim();
  if(subject.length<3||subject.length>160||message.length<10||message.length>5000)return res.code(400).send({code:"INVALID_FEEDBACK",message:"Título ou mensagem inválidos."});
  const now=new Date();
  await productDb().collection("feedback").insertOne({userId:req.user!.sub,email:req.user!.email,name:req.user!.name,category,subject,message,appVersion,platform,status:"new",createdAt:now,updatedAt:now});
  return res.code(201).send({ok:true});
 });
}
