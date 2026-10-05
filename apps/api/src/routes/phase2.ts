import type{FastifyInstance,FastifyReply,FastifyRequest}from"fastify";
import{ObjectId}from"mongodb";
import{productDb}from"../db.js";
import{requireAuth}from"./auth.js";

const columnName=(n:number)=>{let s="";n++;while(n){s=String.fromCharCode(65+(n-1)%26)+s;n=Math.floor((n-1)/26)}return s};
const collections=["charts","tables","pivots","dashboards","comments","shares"] as const;
type CollectionName=typeof collections[number];
const now=()=>new Date().toISOString();
const oid=(v:string)=>ObjectId.isValid(v)?new ObjectId(v):null;

async function access(req:FastifyRequest,workbookId:string,minimum:"viewer"|"commenter"|"editor"="viewer"){
  const id=oid(workbookId); if(!id)return null;
  const db=productDb(),owner=await db.collection("workbooks").findOne({_id:id,ownerId:req.user!.sub},{projection:{_id:1}});
  if(owner)return{role:"owner" as const};
  const share=await db.collection("shares").findOne({workbookId, email:req.user!.email.toLowerCase()});
  if(!share)return null;
  const rank={viewer:1,commenter:2,editor:3} as const;
  return rank[share.permission as keyof typeof rank]>=rank[minimum]?{role:share.permission as "viewer"|"commenter"|"editor"}:null;
}
function sendError(res:FastifyReply,code:string,status=400){return res.code(status).send({code})}
function baseQuery(req:FastifyRequest,workbookId:string){return{workbookId,ownerId:req.user!.sub}}

export async function phase2Routes(app:FastifyInstance){
  for(const name of collections){
    app.get(`/v1/workbooks/:id/${name}`,{preHandler:requireAuth},async(req,res)=>{
      const id=String((req.params as any).id);if(!await access(req,id))return sendError(res,"FORBIDDEN",403);
      const docs=await productDb().collection(name).find({workbookId:id}).sort({updatedAt:-1,createdAt:-1}).limit(500).toArray();
      return res.send(docs.map(x=>({...x,_id:String(x._id)})));
    });
  }

  app.post("/v1/workbooks/:id/charts",{preHandler:requireAuth},async(req,res)=>{
    const id=String((req.params as any).id);if(!await access(req,id,"editor"))return sendError(res,"FORBIDDEN",403);
    const b=(req.body&&typeof req.body==="object"?req.body:{}) as any;
    const d={workbookId:id,ownerId:req.user!.sub,sheetId:String(b.sheetId||""),title:String(b.title||"Gráfico"),type:String(b.type||"bar"),range:String(b.range||"A1:B5"),position:b.position||{x:0,y:0,width:480,height:300},createdAt:now(),updatedAt:now()};
    if(!["bar","line","pie","area","scatter"].includes(d.type))return sendError(res,"INVALID_CHART");
    const r=await productDb().collection("charts").insertOne(d);return res.code(201).send({...d,_id:String(r.insertedId)});
  });

  app.post("/v1/workbooks/:id/tables",{preHandler:requireAuth},async(req,res)=>{
    const id=String((req.params as any).id);if(!await access(req,id,"editor"))return sendError(res,"FORBIDDEN",403);
    const b=(req.body&&typeof req.body==="object"?req.body:{}) as any;
    const d={workbookId:id,ownerId:req.user!.sub,sheetId:String(b.sheetId||""),name:String(b.name||"Tabela"),range:String(b.range||"A1:B5"),headerRow:b.headerRow!==false,filters:{},createdAt:now(),updatedAt:now()};
    const r=await productDb().collection("tables").insertOne(d);return res.code(201).send({...d,_id:String(r.insertedId)});
  });

  app.post("/v1/workbooks/:id/pivots",{preHandler:requireAuth},async(req,res)=>{
    const id=String((req.params as any).id);if(!await access(req,id,"editor"))return sendError(res,"FORBIDDEN",403);
    const b=(req.body&&typeof req.body==="object"?req.body:{}) as any;
    const d={workbookId:id,ownerId:req.user!.sub,sheetId:String(b.sheetId||""),name:String(b.name||"Tabela dinâmica"),sourceRange:String(b.sourceRange||"A1:B5"),rowField:String(b.rowField||""),columnField:b.columnField?String(b.columnField):undefined,valueField:String(b.valueField||""),aggregation:["sum","count","average","min","max"].includes(b.aggregation)?b.aggregation:"sum",createdAt:now(),updatedAt:now()};
    if(!d.rowField||!d.valueField)return sendError(res,"INVALID_PIVOT");
    const wid=oid(id);if(!wid)return sendError(res,"INVALID_ID");
    const wb:any=await productDb().collection("workbooks").findOne({_id:wid});const sheet=wb?.sheets?.find((s:any)=>s.id===d.sheetId)||wb?.sheets?.[0];
    if(!sheet)return sendError(res,"INVALID_SHEET");
    const [ra,rb]=d.sourceRange.split(":");const pm=(k:string)=>{const m=/^([A-Z]+)(\\d+)$/i.exec(k);if(!m)throw Error("RANGE");let col=0;for(const ch of m[1].toUpperCase())col=col*26+ch.charCodeAt(0)-64;return{r:Number(m[2])-1,c:col-1}};const a=pm(ra),z=pm(rb||ra),rows:string[][]=[];
    for(let rr=Math.min(a.r,z.r);rr<=Math.max(a.r,z.r);rr++){const row:string[]=[];for(let cc=Math.min(a.c,z.c);cc<=Math.max(a.c,z.c);cc++)row.push(String(sheet.cells?.[columnName(cc)+(rr+1)]?.input||""));rows.push(row)}
    const header=rows.shift()||[],ri=header.indexOf(d.rowField),vi=header.indexOf(d.valueField);if(ri<0||vi<0)return sendError(res,"INVALID_PIVOT_FIELDS");
    const groups=new Map<string,number[]>();for(const row of rows){const key=row[ri]??"";const n=Number((row[vi]??"").replace(",","."));if(!groups.has(key))groups.set(key,[]);if(Number.isFinite(n))groups.get(key)!.push(n)}
    const result=[...groups.entries()].map(([key,vals])=>{const value=d.aggregation==="count"?vals.length:d.aggregation==="average"?(vals.length?vals.reduce((a,v)=>a+v,0)/vals.length:0):d.aggregation==="min"?(vals.length?Math.min(...vals):0):d.aggregation==="max"?(vals.length?Math.max(...vals):0):vals.reduce((a,v)=>a+v,0);return{key,value}});
    const stored={...d,result};const r=await productDb().collection("pivots").insertOne(stored);return res.code(201).send({...stored,_id:String(r.insertedId)});
  });

  app.post("/v1/workbooks/:id/dashboards",{preHandler:requireAuth},async(req,res)=>{
    const id=String((req.params as any).id);if(!await access(req,id,"editor"))return sendError(res,"FORBIDDEN",403);
    const b=(req.body&&typeof req.body==="object"?req.body:{}) as any;
    const d={workbookId:id,ownerId:req.user!.sub,name:String(b.name||"Dashboard"),charts:Array.isArray(b.charts)?b.charts.map(String):[],tables:Array.isArray(b.tables)?b.tables.map(String):[],refreshMs:Math.max(0,Math.min(3600000,Number(b.refreshMs)||0)),createdAt:now(),updatedAt:now()};
    const r=await productDb().collection("dashboards").insertOne(d);return res.code(201).send({...d,_id:String(r.insertedId)});
  });

  app.post("/v1/workbooks/:id/comments",{preHandler:requireAuth},async(req,res)=>{
    const id=String((req.params as any).id);if(!await access(req,id,"commenter"))return sendError(res,"FORBIDDEN",403);
    const b=(req.body&&typeof req.body==="object"?req.body:{}) as any,body=String(b.body||"").trim();if(!body||body.length>5000)return sendError(res,"INVALID_COMMENT");
    const d={workbookId:id,ownerId:req.user!.sub,sheetId:String(b.sheetId||""),cell:String(b.cell||"A1"),authorId:req.user!.sub,authorEmail:req.user!.email,body,resolved:false,createdAt:now(),updatedAt:now()};
    const r=await productDb().collection("comments").insertOne(d);return res.code(201).send({...d,_id:String(r.insertedId)});
  });

  app.patch("/v1/workbooks/:id/comments/:commentId",{preHandler:requireAuth},async(req,res)=>{
    const id=String((req.params as any).id),cid=oid(String((req.params as any).commentId));if(!cid||!await access(req,id,"commenter"))return sendError(res,"FORBIDDEN",403);
    const b=(req.body&&typeof req.body==="object"?req.body:{}) as any;
    const r=await productDb().collection("comments").findOneAndUpdate({_id:cid,workbookId:id},{$set:{...(typeof b.body==="string"?{body:b.body.trim().slice(0,5000)}:{}),...(typeof b.resolved==="boolean"?{resolved:b.resolved}:{}),updatedAt:now()}},{returnDocument:"after"});
    return r?res.send({...r,_id:String(r._id)}):sendError(res,"NOT_FOUND",404);
  });

  app.post("/v1/workbooks/:id/shares",{preHandler:requireAuth},async(req,res)=>{
    const id=String((req.params as any).id);if(!await access(req,id,"editor"))return sendError(res,"FORBIDDEN",403);
    const b=(req.body&&typeof req.body==="object"?req.body:{}) as any,email=String(b.email||"").trim().toLowerCase(),permission=b.permission;
    if(!/^\S+@\S+\.\S+$/.test(email)||!["viewer","commenter","editor"].includes(permission))return sendError(res,"INVALID_SHARE");
    const d={workbookId:id,ownerId:req.user!.sub,email,permission,createdAt:now(),updatedAt:now()};
    const r=await productDb().collection("shares").findOneAndUpdate({workbookId:id,email},{$set:d,$setOnInsert:{createdAt:d.createdAt}},{upsert:true,returnDocument:"after"});
    if(!r)return sendError(res,"SHARE_FAILED",500);
    return res.send({...r,_id:String(r._id)});
  });

  app.delete("/v1/workbooks/:id/shares/:shareId",{preHandler:requireAuth},async(req,res)=>{
    const id=String((req.params as any).id),sid=oid(String((req.params as any).shareId));if(!sid||!await access(req,id,"editor"))return sendError(res,"FORBIDDEN",403);
    await productDb().collection("shares").deleteOne({_id:sid,workbookId:id});return res.code(204).send();
  });

  app.post("/v1/workbooks/:id/versions",{preHandler:requireAuth},async(req,res)=>{
    const id=String((req.params as any).id);if(!await access(req,id,"editor"))return sendError(res,"FORBIDDEN",403);
    const wid=oid(id);if(!wid)return sendError(res,"INVALID_ID");
    const workbook=await productDb().collection("workbooks").findOne({_id:wid});if(!workbook)return sendError(res,"NOT_FOUND",404);
    const latest=await productDb().collection("versions").find({workbookId:id}).sort({version:-1}).limit(1).next();
    const b=(req.body&&typeof req.body==="object"?req.body:{}) as any;
    const d={workbookId:id,ownerId:String(workbook.ownerId),version:Number(latest?.version||0)+1,label:String(b.label||"Versão"),source:["manual","autosave","sync"].includes(b.source)?b.source:"manual",snapshot:workbook,createdAt:now()};
    const r=await productDb().collection("versions").insertOne(d);return res.code(201).send({...d,_id:String(r.insertedId)});
  });

  app.post("/v1/workbooks/:id/versions/:versionId/restore",{preHandler:requireAuth},async(req,res)=>{
    const id=String((req.params as any).id),vid=oid(String((req.params as any).versionId));if(!vid||!await access(req,id,"editor"))return sendError(res,"FORBIDDEN",403);
    const v=await productDb().collection("versions").findOne({_id:vid,workbookId:id});if(!v)return sendError(res,"NOT_FOUND",404);
    const wid=oid(id);if(!wid)return sendError(res,"INVALID_ID");
    const snap=v.snapshot as any;if(!snap||!Array.isArray(snap.sheets))return sendError(res,"INVALID_VERSION");
    const d={...snap,updatedAt:now(),ownerId:String(snap.ownerId||req.user!.sub)};delete d._id;
    const r=await productDb().collection("workbooks").findOneAndUpdate({_id:wid},{$set:d},{returnDocument:"after"});
    return r?res.send({...r,_id:id}):sendError(res,"NOT_FOUND",404);
  });

  app.get("/v1/workbooks/:id/versions",{preHandler:requireAuth},async(req,res)=>{
    const id=String((req.params as any).id);if(!await access(req,id))return sendError(res,"FORBIDDEN",403);
    const docs=await productDb().collection("versions").find({workbookId:id},{projection:{snapshot:0}}).sort({version:-1}).limit(100).toArray();
    return res.send(docs.map(x=>({...x,_id:String(x._id)})));
  });

  app.post("/v1/workbooks/:id/sync",{preHandler:requireAuth},async(req,res)=>{
    const id=String((req.params as any).id);if(!await access(req,id,"editor"))return sendError(res,"FORBIDDEN",403);
    const b=(req.body&&typeof req.body==="object"?req.body:{}) as any;
    const wid=oid(id);if(!wid)return sendError(res,"INVALID_ID");const existing:any=await productDb().collection("workbooks").findOne({_id:wid});if(!existing)return sendError(res,"NOT_FOUND",404);
    const incoming=b.workbook&&typeof b.workbook==="object"?b.workbook:null;let applied=false;let clean:any=existing;if(incoming&&Array.isArray(incoming.sheets)){clean={...incoming,ownerId:String(existing.ownerId),_id:existing._id,createdAt:existing.createdAt,updatedAt:now()};await productDb().collection("workbooks").replaceOne({_id:wid},clean);applied=true;}
    const d={workbookId:id,ownerId:String(existing.ownerId),sourceDevice:String(b.sourceDevice||"unknown").slice(0,100),clientRevision:Number(b.clientRevision)||0,serverRevision:Number(b.clientRevision)||0,serverTime:now(),status:applied?"synced":"unchanged",workbook:applied?clean:existing};return res.send(d);
  });

  app.post("/v1/workbooks/:id/analysis",{preHandler:requireAuth},async(req,res)=>{
    const id=String((req.params as any).id);if(!await access(req,id))return sendError(res,"FORBIDDEN",403);
    const wid=oid(id);if(!wid)return sendError(res,"INVALID_ID");
    const wb:any=await productDb().collection("workbooks").findOne({_id:wid});if(!wb)return sendError(res,"NOT_FOUND",404);
    const b=(req.body&&typeof req.body==="object"?req.body:{}) as any,sheet=wb.sheets?.find((x:any)=>x.id===b.sheetId)||wb.sheets?.[0];if(!sheet)return sendError(res,"INVALID_SHEET");
    const range=String(b.range||"A1:A1"),[a,z]=range.split(":");const parse=(k:string)=>{const m=/^([A-Z]+)(\d+)$/i.exec(k);if(!m)throw Error("RANGE");let c=0;for(const ch of m[1].toUpperCase())c=c*26+ch.charCodeAt(0)-64;return{r:Number(m[2])-1,c:c-1}};const p=parse(a),q=parse(z||a),rows:string[][]=[];
    for(let r=Math.min(p.r,q.r);r<=Math.max(p.r,q.r);r++){const row:string[]=[];for(let c=Math.min(p.c,q.c);c<=Math.max(p.c,q.c);c++)row.push(String(sheet.cells?.[columnName(c)+(r+1)]?.input||""));rows.push(row)}
    const filters=Array.isArray(b.filters)?b.filters:[];const header=rows.shift()||[],colIndex=(name:string)=>{const n=header.indexOf(name);return n>=0?n:Number(name)};
    let filtered=rows.filter((row:string[])=>filters.every((f:any)=>{const v=row[colIndex(String(f.column))]??"",x=String(f.value??"");switch(f.operator){case"eq":return v===x;case"neq":return v!==x;case"contains":return v.toLowerCase().includes(x.toLowerCase());case"gt":return Number(v.replace(",","."))>Number(x.replace(",","."));case"gte":return Number(v.replace(",","."))>=Number(x.replace(",","."));case"lt":return Number(v.replace(",","."))<Number(x.replace(",","."));case"lte":return Number(v.replace(",","."))<=Number(x.replace(",","."));default:return true}}));
    if(b.sort){const ci=colIndex(String(b.sort.column));filtered.sort((x,y)=>{const nx=Number(x[ci]?.replace(",",".")),ny=Number(y[ci]?.replace(",","."));const cmp=Number.isFinite(nx)&&Number.isFinite(ny)?nx-ny:String(x[ci]??"").localeCompare(String(y[ci]??""));return b.sort.direction==="desc"?-cmp:cmp})}
    const limit=Math.max(1,Math.min(10000,Number(b.limit)||1000));return res.send({headers:header,rows:filtered.slice(0,limit),total:filtered.length});
  });

  app.post("/v1/templates/:id/workbooks",{preHandler:requireAuth},async(req,res)=>{const id=String((req.params as any).id);const templates:any={blank:{name:"Nova planilha"},budget:{name:"Orçamento pessoal"},project:{name:"Controle de projeto"}};const t=templates[id];if(!t)return sendError(res,"NOT_FOUND",404);const nowIso=now();const workbook:any={ownerId:req.user!.sub,name:t.name,schemaVersion:4,sheets:[{id:crypto.randomUUID(),name:"Planilha 1",cells:{},columnWidths:{},rowHeights:{},frozenRows:0,frozenColumns:0,hiddenRows:{},hiddenColumns:{},mergedRanges:[]}],activeSheetId:"",createdAt:nowIso,updatedAt:nowIso};workbook.activeSheetId=workbook.sheets[0].id;if(id==="budget"){workbook.sheets[0].cells={A1:{input:"Mês"},B1:{input:"Receitas"},C1:{input:"Despesas"},D1:{input:"Saldo"}}}if(id==="project"){workbook.sheets[0].cells={A1:{input:"Tarefa"},B1:{input:"Responsável"},C1:{input:"Prazo"},D1:{input:"Status"}}}const r=await productDb().collection("workbooks").insertOne(workbook);return res.code(201).send({...workbook,_id:String(r.insertedId)});});

  app.get("/v1/templates",{preHandler:requireAuth},async(_req,res)=>{
    return res.send([
      {id:"blank",name:"Em branco",description:"Planilha vazia",category:"Geral",workbook:{name:"Nova planilha",schemaVersion:4}},
      {id:"budget",name:"Orçamento pessoal",description:"Receitas e despesas mensais",category:"Finanças",workbook:{name:"Orçamento pessoal",schemaVersion:4}},
      {id:"project",name:"Controle de projeto",description:"Tarefas, responsáveis e prazos",category:"Projetos",workbook:{name:"Controle de projeto",schemaVersion:4}}
    ]);
  });
}
