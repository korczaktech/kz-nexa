import{readFileSync}from"node:fs";
const p=readFileSync(new URL("./app/src/main/java/com/korczaktech/nexa/MainActivity.kt",import.meta.url),"utf8");
for(const x of ["phase2()","phase2Chart","phase2Table","phase2Pivot","phase2Dashboard","phase2Comment","phase2Share","phase2Version","phase2Analysis","phase2Templates","phase2Sync"])if(!p.includes(x))throw new Error("Android nativo ausente: "+x);
if(/WebView|loadUrl\(/.test(p))throw new Error("Android da Fase 2 não pode depender de WebView.");
console.log("phase2 android verification: PASS");