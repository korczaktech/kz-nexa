package com.korczaktech.nexa
import android.app.*;import android.os.*;import android.content.*;import android.net.Uri;import android.graphics.*;import android.view.*;import android.widget.*;import org.json.*;import java.net.*;import java.util.*;import java.io.*;import java.text.SimpleDateFormat
private const val API="https://kz-nexa.onrender.com"
private data class Cell(var input:String="",var bold:Boolean=false,var italic:Boolean=false,var underline:Boolean=false,var strike:Boolean=false,var align:Int=0,var numberFormat:String="general",var fontSize:Float=14f,var fontColor:Int=Color.DKGRAY,var background:Int=Color.WHITE,var borderTop:Boolean=false,var borderRight:Boolean=false,var borderBottom:Boolean=false,var borderLeft:Boolean=false,var wrap:Boolean=false)
private data class Rule(var range:String,var op:String,var value:String,var bg:Int=Color.rgb(22,77,49),var fg:Int=Color.rgb(109,255,173))
private data class Validation(var type:String,var values:List<String> = emptyList(),var min:Double?=null,var max:Double?=null)
private data class Sheet(val id:String=UUID.randomUUID().toString(),var name:String,var cells:MutableMap<String,Cell> = mutableMapOf(),var frozenRows:Int=0,var frozenCols:Int=0,var hiddenRows:MutableSet<Int> = mutableSetOf(),var hiddenCols:MutableSet<Int> = mutableSetOf(),var merged:MutableSet<String> = mutableSetOf(),var rules:MutableList<Rule> = mutableListOf(),var validations:MutableMap<String,Validation> = mutableMapOf(),var groupedRows:MutableSet<Int> = mutableSetOf(),var groupedCols:MutableSet<Int> = mutableSetOf())
private data class Book(var id:String?=null,var name:String="Nova planilha",var sheets:MutableList<Sheet>,var active:Int=0)
class MainActivity:Activity(){
 private var token:String?=null;private var uid="";private var book:Book?=null;private val PICK=91;private val SAVE=92;private lateinit var root:FrameLayout;private lateinit var grid:Grid;private val APP_VERSION="0.0.0.1"
 override fun onCreate(b:Bundle?){super.onCreate(b);root=FrameLayout(this);setContentView(root);token=getPreferences(0).getString("token",null);uid=getPreferences(0).getString("uid","")?:"";if(token==null)login()else load();checkForUpdate()}
 private fun checkForUpdate(){
  Thread{try{
   val c=URL("https://api.github.com/repos/korczaktech/kz-nexa/releases/latest").openConnection() as HttpURLConnection
   c.requestMethod="GET";c.setRequestProperty("Accept","application/vnd.github+json");c.connectTimeout=8000;c.readTimeout=10000
   if(c.responseCode !in 200..299)return@Thread
   val j=JSONObject(c.inputStream.bufferedReader().use{it.readText()});val tag=j.optString("tag_name").removePrefix("v")
   val assets=j.optJSONArray("assets");var apk:String?=null
   if(assets!=null)for(i in 0 until assets.length()){val a=assets.getJSONObject(i);if(a.optString("name").lowercase().endsWith(".apk")){apk=a.optString("browser_download_url");break}}
   if(apk.isNullOrBlank()||!isNewer(tag,APP_VERSION))return@Thread
   runOnUiThread{AlertDialog.Builder(this).setTitle("Atualização disponível").setMessage("O Nexa $tag está disponível. Deseja atualizar agora?").setPositiveButton("Atualizar"){_,_->downloadUpdate(apk!!,tag)}.setNegativeButton("Agora não",null).show()}
  }catch(_:Exception){}}.start()
 }
 private fun isNewer(remote:String,current:String):Boolean{
  val a=remote.split(".").map{it.filter{c->c.isDigit()}.toIntOrNull()?:0}.toMutableList();val b=current.split(".").map{it.filter{c->c.isDigit()}.toIntOrNull()?:0}.toMutableList();while(a.size<4)a.add(0);while(b.size<4)b.add(0);
  for(i in 0 until 4)if(a[i]!=b[i])return a[i]>b[i];return false
 }
 private fun downloadUpdate(url:String,tag:String){
  Toast.makeText(this,"Baixando Nexa $tag...",Toast.LENGTH_LONG).show()
  Thread{try{
   val dm=getSystemService(DOWNLOAD_SERVICE) as android.app.DownloadManager
   val rq=android.app.DownloadManager.Request(Uri.parse(url)).setTitle("Korczak Nexa $tag").setDescription("Atualização do aplicativo").setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED).setMimeType("application/vnd.android.package-archive").setDestinationInExternalFilesDir(this,android.os.Environment.DIRECTORY_DOWNLOADS,"korczak-nexa-$tag.apk")
   val id=dm.enqueue(rq);android.os.Handler(mainLooper).post(object:Runnable{override fun run(){
    val q=dm.query(android.app.DownloadManager.Query().setFilterById(id));if(!q.moveToFirst()){q.close();android.os.Handler(mainLooper).postDelayed(this,1000);return}
    val status=q.getInt(q.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_STATUS));q.close()
    if(status==android.app.DownloadManager.STATUS_SUCCESSFUL){val uri=dm.getUriForDownloadedFile(id);if(uri!=null)startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri,"application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));return}
    if(status==android.app.DownloadManager.STATUS_FAILED){Toast.makeText(this@MainActivity,"Falha ao baixar a atualização.",Toast.LENGTH_LONG).show();return}
    android.os.Handler(mainLooper).postDelayed(this,1000)
   }})
  }catch(_:Exception){runOnUiThread{Toast.makeText(this,"Falha ao iniciar atualização.",Toast.LENGTH_LONG).show()}}}.start()
 }
 private fun login(){root.removeAllViews();val l=LinearLayout(this);l.orientation=LinearLayout.VERTICAL;l.setPadding(48,64,48,48);l.setBackgroundColor(Color.rgb(6,16,11));val t=TextView(this);t.text="Korczak Nexa";t.textSize=30f;t.setTextColor(Color.WHITE);l.addView(t);val e=EditText(this);e.hint="Email";l.addView(e);val p=EditText(this);p.hint="Senha";p.inputType=129;l.addView(p);val err=TextView(this);err.setTextColor(Color.RED);l.addView(err);val bt=Button(this);bt.text="Entrar";l.addView(bt);root.addView(l);bt.setOnClickListener{Thread{try{val r=req("/v1/auth/login","POST",JSONObject().put("email",e.text.toString()).put("password",p.text.toString()).toString(),null);if(r.first !in 200..299)throw Exception(JSONObject(r.second).optString("message","Falha no login"));val j=JSONObject(r.second);token=j.getString("token");uid=j.getJSONObject("user").getString("id");getPreferences(0).edit().putString("token",token).putString("uid",uid).apply();runOnUiThread{load()}}catch(x:Exception){runOnUiThread{err.text=x.message;bt.isEnabled=true}}}.start()}}
 private fun load(){Thread{try{val r=req("/v1/workbooks","GET",null,token);val a=JSONArray(r.second);book=if(a.length()>0)from(a.getJSONObject(0))else Book(sheets=mutableListOf(Sheet(name="Planilha 1")));runOnUiThread{editor()}}catch(x:Exception){runOnUiThread{Toast.makeText(this,"Falha ao carregar",Toast.LENGTH_LONG).show();login()}}}.start()}
 private fun editor(){root.removeAllViews();val l=LinearLayout(this);l.orientation=LinearLayout.VERTICAL;val bar=LinearLayout(this);fun b(s:String,f:()->Unit){Button(this).also{x->x.text=s;x.setOnClickListener{f()};bar.addView(x)}};b("Salvar"){save()};b("Nexa Completo"){phase2()};b("↶"){undo()};b("↷"){redo()};b("+ Aba"){addSheet()};b("Mesclar"){merge()};b("Desmesclar"){unmerge()};b("Congelar"){freeze()};b("Ocultar"){hide()};b("Mostrar"){show()};b("Zoom +"){grid.zoom*=1.15f;grid.invalidate()};b("Zoom -"){grid.zoom=maxOf(.55f,grid.zoom/1.15f);grid.invalidate()};b("B"){toggle("b")};b("I"){toggle("i")};b("U"){toggle("u")};b("S"){toggle("s")};b("←"){align(0)};b("↔"){align(1)};b("→"){align(2)};b("Tamanho"){fontSize()};b("Quebra"){wrap()};b("Bordas"){border()};b("Condicional"){conditional()};b("Validação"){validation()};b("Agrupar linha"){groupRow()};b("Agrupar coluna"){groupCol()};b("Grupos +/-"){toggleGroups()};b("Número"){numberFormat()};b("Preencher"){fill()};b("Copiar"){copy()};b("Colar"){paste()};b("Importar"){importFile()};b("Exportar"){exportFile()};b("Sair"){getPreferences(0).edit().clear().apply();token=null;login()};l.addView(bar);grid=Grid();l.addView(grid,LinearLayout.LayoutParams(-1,0,1f));root.addView(l)}
 private val history=ArrayDeque<String>();private val future=ArrayDeque<String>();private val collapsedRows=mutableSetOf<Int>();private val collapsedCols=mutableSetOf<Int>()
 private fun snap(){history.addLast(toJson(book!!).toString());if(history.size>50)history.removeFirst();future.clear()}
 private fun undo(){if(history.isEmpty())return;future.addFirst(toJson(book!!).toString());book=from(JSONObject(history.removeLast()));grid.invalidate()}
 private fun redo(){if(future.isEmpty())return;history.addLast(toJson(book!!).toString());book=from(JSONObject(future.removeFirst()));grid.invalidate()}
 private fun addSheet(){snap();book!!.sheets.add(Sheet(name="Planilha "+(book!!.sheets.size+1)));book!!.active=book!!.sheets.lastIndex;grid.invalidate()}
 private fun edit(r:Int,c:Int){val s=book!!.sheets[book!!.active];val k=key(r,c);val ce=s.cells[k]?:Cell();val input=EditText(this);input.setText(ce.input);input.selectAll();AlertDialog.Builder(this).setTitle(col(c)+(r+1)).setView(input).setPositiveButton("OK"){_,_->val vv=input.text.toString();if(!valid(vv,s.validations[k])){Toast.makeText(this,"Valor inválido",Toast.LENGTH_SHORT).show();return@setPositiveButton};snap();if(vv.isEmpty())s.cells.remove(k)else{ce.input=vv;s.cells[k]=ce};grid.invalidate()}.setNegativeButton("Cancelar",null).show()}
 private fun toggle(t:String){snap();val s=book!!.sheets[book!!.active];for(r in grid.selStart..grid.selEnd)for(k in grid.selColStart..grid.selColEnd){val ce=s.cells[key(r,k)]?:Cell();if(t=="b")ce.bold=!ce.bold;if(t=="i")ce.italic=!ce.italic;if(t=="u")ce.underline=!ce.underline;if(t=="s")ce.strike=!ce.strike;s.cells[key(r,k)]=ce};grid.invalidate()}
 private fun align(a:Int){snap();val s=book!!.sheets[book!!.active];for(r in grid.selStart..grid.selEnd)for(k in grid.selColStart..grid.selColEnd){val ce=s.cells[key(r,k)]?:Cell();ce.align=a;s.cells[key(r,k)]=ce};grid.invalidate()}
 private fun fontSize(){val input=EditText(this);input.inputType=2;input.setText("14");AlertDialog.Builder(this).setTitle("Tamanho da fonte").setView(input).setPositiveButton("Aplicar"){_,_->val n=input.text.toString().toFloatOrNull()?:14f;snap();val s=book!!.sheets[book!!.active];for(r in grid.selStart..grid.selEnd)for(k in grid.selColStart..grid.selColEnd){val ce=s.cells[key(r,k)]?:Cell();ce.fontSize=n.coerceIn(8f,72f);s.cells[key(r,k)]=ce};grid.invalidate()}.setNegativeButton("Cancelar",null).show()}
 private fun wrap(){snap();val s=book!!.sheets[book!!.active];for(r in grid.selStart..grid.selEnd)for(k in grid.selColStart..grid.selColEnd){val ce=s.cells[key(r,k)]?:Cell();ce.wrap=!ce.wrap;s.cells[key(r,k)]=ce};grid.invalidate()}
 private fun border(){snap();val s=book!!.sheets[book!!.active];for(r in grid.selStart..grid.selEnd)for(k in grid.selColStart..grid.selColEnd){val ce=s.cells[key(r,k)]?:Cell();ce.borderTop=true;ce.borderRight=true;ce.borderBottom=true;ce.borderLeft=true;s.cells[key(r,k)]=ce};grid.invalidate()}
 private fun conditional(){val s=book!!.sheets[book!!.active];val box=LinearLayout(this);box.orientation=LinearLayout.VERTICAL;val op=Spinner(this);op.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,arrayOf("eq","neq","contains","gt","gte","lt","lte"));val v=EditText(this);v.hint="Valor";box.addView(op);box.addView(v);AlertDialog.Builder(this).setTitle("Formatação condicional").setView(box).setPositiveButton("Aplicar"){_,_->snap();s.rules.add(Rule(range(grid.selStart,grid.selColStart,grid.selEnd,grid.selColEnd),op.selectedItem.toString(),v.text.toString()));grid.invalidate()}.setNegativeButton("Cancelar",null).show()}
 private fun validation(){val s=book!!.sheets[book!!.active];val box=LinearLayout(this);box.orientation=LinearLayout.VERTICAL;val type=Spinner(this);type.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,arrayOf("text","number","date","list"));val vals=EditText(this);vals.hint="Lista: A, B, C";val min=EditText(this);min.hint="Mínimo";val max=EditText(this);max.hint="Máximo";box.addView(type);box.addView(vals);box.addView(min);box.addView(max);AlertDialog.Builder(this).setTitle("Validação").setView(box).setPositiveButton("Aplicar"){_,_->snap();val t=type.selectedItem.toString();s.validations[key(grid.selStart,grid.selColStart)]=Validation(t,vals.text.toString().split(",").map{it.trim()}.filter{it.isNotEmpty()},min.text.toString().toDoubleOrNull(),max.text.toString().toDoubleOrNull());grid.invalidate()}.setNegativeButton("Cancelar",null).show()}
 private fun groupRow(){snap();val s=book!!.sheets[book!!.active];for(i in grid.selStart..grid.selEnd)s.groupedRows.add(i);grid.invalidate()}
 private fun groupCol(){snap();val s=book!!.sheets[book!!.active];for(i in grid.selColStart..grid.selColEnd)s.groupedCols.add(i);grid.invalidate()}
 private fun toggleGroups(){val s=book!!.sheets[book!!.active];if(s.groupedRows.isNotEmpty()){val start=s.groupedRows.minOrNull()!!;if(collapsedRows.contains(start))collapsedRows.remove(start)else collapsedRows.add(start)};if(s.groupedCols.isNotEmpty()){val start=s.groupedCols.minOrNull()!!;if(collapsedCols.contains(start))collapsedCols.remove(start)else collapsedCols.add(start)};grid.invalidate()}
 private fun range(r1:Int,c1:Int,r2:Int,c2:Int)=key(minOf(r1,r2),minOf(c1,c2))+":"+key(maxOf(r1,r2),maxOf(c1,c2))
 private fun valid(v:String,r:Validation?):Boolean{if(r==null)return true;return when(r.type){"text"->true;"number"->v.toDoubleOrNull()?.let{x->(r.min==null||x>=r.min!!) && (r.max==null||x<=r.max!!)}?:false;"date"->try{val d=java.text.SimpleDateFormat("yyyy-MM-dd").parse(v)?:return false;val t=d.time;(r.min==null||t>=r.min!!) && (r.max==null||t<=r.max!!)}catch(_:Exception){false};"list"->r.values.contains(v);else->true}}
 private fun numberFormat(){snap();val s=book!!.sheets[book!!.active];for(r in grid.selStart..grid.selEnd)for(k in grid.selColStart..grid.selColEnd){val ce=s.cells[key(r,k)]?:Cell();ce.numberFormat=when(ce.numberFormat){"general"->"number";"number"->"currency";"currency"->"percent";else->"general"};s.cells[key(r,k)]=ce};grid.invalidate()}
 private fun fill(){snap();val s=book!!.sheets[book!!.active];val src=s.cells[key(grid.selStart,grid.selColStart)]?.input?:"";for(r in grid.selStart..grid.selEnd)for(k in grid.selColStart..grid.selColEnd){if(r==grid.selStart&&k==grid.selColStart)continue;val v=if(src.startsWith("="))shiftFormula(src,r-grid.selStart,k-grid.selColStart)else src;s.cells[key(r,k)]=Cell(v)};grid.invalidate()}
 private fun shiftFormula(f:String,dr:Int,dc:Int):String=f.replace(Regex("(\\$?)([A-Z]+)(\\$?)([0-9]+)")){m->var n=0;for(ch in m.groupValues[2])n=n*26+ch.code-64;val ac=m.groupValues[1]=="$";val ar=m.groupValues[3]=="$";val rr=m.groupValues[4].toInt()+if(ar)0 else dr;val cc=n-1+if(ac)0 else dc;if(rr<1||cc<0)m.value else (if(ac)"$" else "")+col(cc)+(if(ar)"$" else "")+rr}
 private fun copy(){val s=book!!.sheets[book!!.active];val rows=(grid.selStart..grid.selEnd).map{r->(grid.selColStart..grid.selColEnd).joinToString("\t"){k->s.cells[key(r,k)]?.input?:""}};val cm=getSystemService(CLIPBOARD_SERVICE) as ClipboardManager;cm.setPrimaryClip(ClipData.newPlainText("Nexa",rows.joinToString("\n")))}
 private fun paste(){val cm=getSystemService(CLIPBOARD_SERVICE) as ClipboardManager;val v=cm.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString()?:return;val rows=v.replace("\r\n","\n").replace("\r","\n").split("\n").map{it.split("\t")};snap();val s=book!!.sheets[book!!.active];for((dr,row) in rows.withIndex())for((dc,value) in row.withIndex())s.cells[key(grid.selStart+dr,grid.selColStart+dc)]=Cell(value);grid.invalidate()}
 private fun importFile(){startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE),PICK)}
 private fun exportFile(){val opts=arrayOf("Nexa (.nexa)","CSV (.csv)","TSV (.tsv)");AlertDialog.Builder(this).setTitle("Exportar").setItems(opts){_,which->val ext=if(which==0)"nexa" else if(which==1)"csv" else "tsv";startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).setType("text/plain").putExtra(Intent.EXTRA_TITLE,(book!!.name.ifBlank{"nexa"})+"."+ext),SAVE+which)}.show()}
 override fun onActivityResult(q:Int,res:Int,data:Intent?){super.onActivityResult(q,res,data);if(res!=RESULT_OK||data?.data==null)return;try{if(q==PICK){val raw=contentResolver.openInputStream(data.data!!)?.bufferedReader()?.use{it.readText()}?:return;book=if(raw.trimStart().startsWith("{"))from(JSONObject(raw))else fromDelimited(raw,if(data.data!!.toString().lowercase().contains("tsv"))"\t" else ",");grid.invalidate()}else{val ext=when(q){SAVE->"nexa";SAVE+1->"csv";else->"tsv"};val out=contentResolver.openOutputStream(data.data!!)?:return;val bytes=if(ext=="nexa")toJson(book!!).toString(2).toByteArray() else delimited(book!!.sheets[book!!.active],if(ext=="csv")"," else "\t").toByteArray();out.use{it.write(bytes)}}}catch(_:Exception){Toast.makeText(this,"Arquivo inválido",Toast.LENGTH_LONG).show()}}
 private fun delimited(s:Sheet,d:String):String{val maxR=(s.cells.keys.mapNotNull{Regex("[A-Z]+([0-9]+)").matchEntire(it)?.groupValues?.get(1)?.toIntOrNull()}.maxOrNull()?:1);val maxC=(s.cells.keys.mapNotNull{Regex("([A-Z]+)[0-9]+").matchEntire(it)?.groupValues?.get(1)?.let{v->var n=0;for(ch in v)n=n*26+ch.code-64;n}}.maxOrNull()?:1);return (0 until maxR).joinToString("\n"){r->(0 until maxC).joinToString(d){c->s.cells[key(r,c)]?.input?.replace(d," ")?.replace("\n"," ")?:""}}}
 private fun fromDelimited(raw:String,d:String):Book{val rows=raw.replace("\r\n","\n").replace("\r","\n").split("\n");val s=Sheet(name="Planilha 1");for((r,line) in rows.withIndex())for((c,v) in line.split(d).withIndex())if(v.isNotEmpty())s.cells[key(r,c)]=Cell(v);return Book(sheets=mutableListOf(s))}
 private fun merge(){snap();book!!.sheets[book!!.active].merged.add(grid.selColStart.toString()+","+grid.selStart+":"+grid.selColEnd+","+grid.selEnd);grid.invalidate()}
 private fun unmerge(){snap();book!!.sheets[book!!.active].merged.clear();grid.invalidate()}
 private fun freeze(){snap();val s=book!!.sheets[book!!.active];s.frozenRows=grid.selStart+1;s.frozenCols=grid.selColStart+1;grid.invalidate()}
 private fun hide(){snap();val s=book!!.sheets[book!!.active];for(i in grid.selStart..grid.selEnd)s.hiddenRows.add(i);for(i in grid.selColStart..grid.selColEnd)s.hiddenCols.add(i);grid.invalidate()}
 private fun show(){snap();val s=book!!.sheets[book!!.active];s.hiddenRows.clear();s.hiddenCols.clear();grid.invalidate()}
 private fun phase2(){
  val b=book?:return
  if(b.id==null){Toast.makeText(this,"Salve a planilha antes de usar as ferramentas da Fase 2.",Toast.LENGTH_LONG).show();return}
  val items=arrayOf("Gráfico","Tabela","Tabela dinâmica","Dashboard","Comentário","Compartilhar","Nova versão","Analisar dados","Modelos","Sincronizar")
  AlertDialog.Builder(this).setTitle("Nexa Completo").setItems(items){_,which->
   when(which){
    0->phase2Chart(b.id!!);1->phase2Table(b.id!!);2->phase2Pivot(b.id!!);3->phase2Dashboard(b.id!!);4->phase2Comment(b.id!!);5->phase2Share(b.id!!);6->phase2Version(b.id!!);7->phase2Analysis(b.id!!);8->phase2Templates();9->phase2Sync(b.id!!)
   }
  }.setNegativeButton("Fechar",null).show()
 }
 private fun phase2Range()=range(grid.selStart,grid.selColStart,grid.selEnd,grid.selColEnd)
 private fun phase2Chart(id:String){
  val box=LinearLayout(this);box.orientation=LinearLayout.VERTICAL
  val title=EditText(this);title.hint="Título";box.addView(title)
  val type=Spinner(this);type.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,arrayOf("bar","line","area","pie","scatter"));box.addView(type)
  AlertDialog.Builder(this).setTitle("Criar gráfico").setView(box).setPositiveButton("Criar"){_,_->Thread{try{val j=JSONObject().put("sheetId",book!!.sheets[book!!.active].id).put("title",title.text.toString().ifBlank{"Gráfico"}).put("type",type.selectedItem.toString()).put("range",phase2Range());val r=req("/v1/workbooks/$id/charts","POST",j.toString(),token);runOnUiThread{Toast.makeText(this,if(r.first in 200..299)"Gráfico criado" else "Falha ao criar gráfico",Toast.LENGTH_SHORT).show()}}catch(_:Exception){runOnUiThread{Toast.makeText(this,"Falha de rede",Toast.LENGTH_SHORT).show()}}}}.setNegativeButton("Cancelar",null).show()
 }
 private fun phase2Table(id:String){Thread{try{val j=JSONObject().put("sheetId",book!!.sheets[book!!.active].id).put("name","Tabela").put("range",phase2Range()).put("headerRow",true);val r=req("/v1/workbooks/$id/tables","POST",j.toString(),token);runOnUiThread{Toast.makeText(this,if(r.first in 200..299)"Tabela criada" else "Falha ao criar tabela",Toast.LENGTH_SHORT).show()}}catch(_:Exception){runOnUiThread{Toast.makeText(this,"Falha de rede",Toast.LENGTH_SHORT).show()}}}.start()}
 private fun phase2Pivot(id:String){
  val box=LinearLayout(this);box.orientation=LinearLayout.VERTICAL;val row=EditText(this);row.hint="Campo de linha";val value=EditText(this);value.hint="Campo de valor";val agg=Spinner(this);agg.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,arrayOf("sum","count","average","min","max"));box.addView(row);box.addView(value);box.addView(agg)
  AlertDialog.Builder(this).setTitle("Tabela dinâmica").setView(box).setPositiveButton("Criar"){_,_->Thread{try{val j=JSONObject().put("sheetId",book!!.sheets[book!!.active].id).put("name","Tabela dinâmica").put("sourceRange",phase2Range()).put("rowField",row.text.toString()).put("valueField",value.text.toString()).put("aggregation",agg.selectedItem.toString());val r=req("/v1/workbooks/$id/pivots","POST",j.toString(),token);runOnUiThread{Toast.makeText(this,if(r.first in 200..299)"Tabela dinâmica criada" else "Falha",Toast.LENGTH_SHORT).show()}}catch(_:Exception){runOnUiThread{Toast.makeText(this,"Falha de rede",Toast.LENGTH_SHORT).show()}}}}.setNegativeButton("Cancelar",null).show()
 }
 private fun phase2Dashboard(id:String){Thread{try{val j=JSONObject().put("name","Dashboard").put("charts",JSONArray()).put("tables",JSONArray()).put("refreshMs",0);val r=req("/v1/workbooks/$id/dashboards","POST",j.toString(),token);runOnUiThread{Toast.makeText(this,if(r.first in 200..299)"Dashboard criado" else "Falha",Toast.LENGTH_SHORT).show()}}catch(_:Exception){runOnUiThread{Toast.makeText(this,"Falha de rede",Toast.LENGTH_SHORT).show()}}}.start()}
 private fun phase2Comment(id:String){
  val input=EditText(this);input.hint="Comentário"
  AlertDialog.Builder(this).setTitle("Comentário em "+key(grid.selStart,grid.selColStart)).setView(input).setPositiveButton("Adicionar"){_,_->Thread{try{val j=JSONObject().put("sheetId",book!!.sheets[book!!.active].id).put("cell",key(grid.selStart,grid.selColStart)).put("body",input.text.toString());val r=req("/v1/workbooks/$id/comments","POST",j.toString(),token);runOnUiThread{Toast.makeText(this,if(r.first in 200..299)"Comentário adicionado" else "Falha",Toast.LENGTH_SHORT).show()}}catch(_:Exception){runOnUiThread{Toast.makeText(this,"Falha de rede",Toast.LENGTH_SHORT).show()}}}}.setNegativeButton("Cancelar",null).show()
 }
 private fun phase2Share(id:String){
  val box=LinearLayout(this);box.orientation=LinearLayout.VERTICAL;val email=EditText(this);email.hint="Email";val permission=Spinner(this);permission.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,arrayOf("viewer","commenter","editor"));box.addView(email);box.addView(permission)
  AlertDialog.Builder(this).setTitle("Compartilhar").setView(box).setPositiveButton("Compartilhar"){_,_->Thread{try{val j=JSONObject().put("email",email.text.toString()).put("permission",permission.selectedItem.toString());val r=req("/v1/workbooks/$id/shares","POST",j.toString(),token);runOnUiThread{Toast.makeText(this,if(r.first in 200..299)"Compartilhamento atualizado" else "Falha",Toast.LENGTH_SHORT).show()}}catch(_:Exception){runOnUiThread{Toast.makeText(this,"Falha de rede",Toast.LENGTH_SHORT).show()}}}}.setNegativeButton("Cancelar",null).show()
 }
 private fun phase2Version(id:String){Thread{try{val r=req("/v1/workbooks/$id/versions","POST",JSONObject().put("label","Versão manual").put("source","manual").toString(),token);runOnUiThread{Toast.makeText(this,if(r.first in 200..299)"Versão registrada" else "Falha",Toast.LENGTH_SHORT).show()}}catch(_:Exception){runOnUiThread{Toast.makeText(this,"Falha de rede",Toast.LENGTH_SHORT).show()}}}.start()}
 private fun phase2Analysis(id:String){Thread{try{val j=JSONObject().put("sheetId",book!!.sheets[book!!.active].id).put("range",phase2Range()).put("limit",1000);val r=req("/v1/workbooks/$id/analysis","POST",j.toString(),token);runOnUiThread{AlertDialog.Builder(this).setTitle("Análise de dados").setMessage(if(r.first in 200..299)r.second else "Falha ao analisar").setPositiveButton("OK",null).show()}}catch(_:Exception){runOnUiThread{Toast.makeText(this,"Falha de rede",Toast.LENGTH_SHORT).show()}}}.start()}
 private fun phase2Templates(){Thread{try{val r=req("/v1/templates","GET",null,token);runOnUiThread{if(r.first !in 200..299){Toast.makeText(this,"Falha ao carregar modelos",Toast.LENGTH_SHORT).show();return@runOnUiThread};val arr=JSONObject().runCatching{JSONArray(r.second)}.getOrNull();if(arr==null||arr.length()==0){Toast.makeText(this,"Nenhum modelo disponível",Toast.LENGTH_SHORT).show();return@runOnUiThread};val names=Array(arr.length()){i->arr.getJSONObject(i).optString("name")};AlertDialog.Builder(this).setTitle("Modelos").setItems(names){_,which->val id=arr.getJSONObject(which).optString("id");Thread{try{val cr=req("/v1/templates/$id/workbooks","POST","{}",token);runOnUiThread{Toast.makeText(this,if(cr.first in 200..299)"Modelo criado" else "Falha ao criar modelo",Toast.LENGTH_SHORT).show()}}catch(_:Exception){runOnUiThread{Toast.makeText(this,"Falha de rede",Toast.LENGTH_SHORT).show()}}}.start()}.setNegativeButton("Fechar",null).show()}}catch(_:Exception){runOnUiThread{Toast.makeText(this,"Falha de rede",Toast.LENGTH_SHORT).show()}}}.start()}
 private fun phase2Sync(id:String){Thread{try{val j=JSONObject().put("sourceDevice","android-native").put("clientRevision",System.currentTimeMillis()).put("workbook",toJson(book!!));val r=req("/v1/workbooks/$id/sync","POST",j.toString(),token);runOnUiThread{Toast.makeText(this,if(r.first in 200..299)"Sincronização concluída" else "Falha na sincronização",Toast.LENGTH_SHORT).show()}}catch(_:Exception){runOnUiThread{Toast.makeText(this,"Falha de rede",Toast.LENGTH_SHORT).show()}}}.start()}
 private fun save(){val b=book?:return;Thread{try{val r=req(if(b.id==null)"/v1/workbooks" else "/v1/workbooks/"+b.id,if(b.id==null)"POST" else "PUT",toJson(b).toString(),token);if(r.first !in 200..299)throw Exception();if(b.id==null)book=from(JSONObject(r.second));runOnUiThread{Toast.makeText(this,"Salvo",Toast.LENGTH_SHORT).show()}}catch(_:Exception){runOnUiThread{Toast.makeText(this,"Falha ao salvar",Toast.LENGTH_LONG).show()}}}.start()}
 private fun key(r:Int,c:Int)=col(c)+(r+1)
 private fun col(i:Int):String{var n=i+1;var z="";while(n>0){z=Char(65+(n-1)%26)+z;n=(n-1)/26};return z}
 private fun toJson(b:Book):JSONObject{val j=JSONObject().put("ownerId",uid).put("name",b.name).put("schemaVersion",4).put("activeSheetId",b.sheets[b.active].id);val sa=JSONArray();for(s in b.sheets){val o=JSONObject().put("id",s.id).put("name",s.name).put("frozenRows",s.frozenRows).put("frozenColumns",s.frozenCols);val cs=JSONObject();for((k,c) in s.cells)cs.put(k,JSONObject().put("input",c.input).put("style",JSONObject().put("bold",c.bold).put("italic",c.italic).put("underline",c.underline).put("strike",c.strike).put("align",c.align).put("numberFormat",c.numberFormat).put("fontSize",c.fontSize).put("fontColor",c.fontColor).put("backgroundColor",c.background).put("borderTop",c.borderTop).put("borderRight",c.borderRight).put("borderBottom",c.borderBottom).put("borderLeft",c.borderLeft).put("wrap",c.wrap)));val rs=JSONArray();for(rule in s.rules)rs.put(JSONObject().put("range",rule.range).put("op",rule.op).put("value",rule.value).put("bg",rule.bg).put("fg",rule.fg));val vs=JSONObject();for((k,v) in s.validations)vs.put(k,JSONObject().put("type",v.type).put("values",JSONArray(v.values)).put("min",v.min).put("max",v.max));o.put("conditionalRules",rs).put("validationRules",vs).put("cells",cs).put("columnWidths",JSONObject()).put("rowHeights",JSONObject()).put("hiddenRows",JSONArray(s.hiddenRows.toList())).put("hiddenColumns",JSONArray(s.hiddenCols.toList())).put("mergedRanges",JSONArray(s.merged.toList())).put("groupedRows",JSONArray(s.groupedRows.toList())).put("groupedColumns",JSONArray(s.groupedCols.toList()));sa.put(o)};return j.put("sheets",sa)}
 private fun from(j:JSONObject):Book{
  val a=j.optJSONArray("sheets")?:JSONArray();val ss=mutableListOf<Sheet>()
  for(i in 0 until a.length()){
   val o=a.getJSONObject(i);val s=Sheet(o.optString("id",UUID.randomUUID().toString()),o.optString("name","Planilha "+(i+1)))
   val cs=o.optJSONObject("cells")?:JSONObject()
   for(k in cs.keys()){val c=cs.getJSONObject(k);val st=c.optJSONObject("style");s.cells[k]=Cell(c.optString("input"),st?.optBoolean("bold")?:false,st?.optBoolean("italic")?:false,st?.optBoolean("underline")?:false,st?.optBoolean("strike")?:false,st?.optInt("align")?:0,st?.optString("numberFormat","general")?:"general",st?.optDouble("fontSize",14.0)?.toFloat()?:14f,st?.optInt("fontColor",Color.DKGRAY)?:Color.DKGRAY,st?.optInt("backgroundColor",Color.WHITE)?:Color.WHITE,st?.optBoolean("borderTop")?:false,st?.optBoolean("borderRight")?:false,st?.optBoolean("borderBottom")?:false,st?.optBoolean("borderLeft")?:false,st?.optBoolean("wrap")?:false)}
   s.frozenRows=o.optInt("frozenRows");s.frozenCols=o.optInt("frozenColumns")
   o.optJSONArray("hiddenRows")?.let{x->for(n in 0 until x.length())s.hiddenRows.add(x.getInt(n))}
   o.optJSONArray("hiddenColumns")?.let{x->for(n in 0 until x.length())s.hiddenCols.add(x.getInt(n))}
   o.optJSONArray("mergedRanges")?.let{x->for(n in 0 until x.length())s.merged.add(x.getString(n))}
   o.optJSONArray("groupedRows")?.let{x->for(n in 0 until x.length())s.groupedRows.add(x.getInt(n))}
   o.optJSONArray("groupedColumns")?.let{x->for(n in 0 until x.length())s.groupedCols.add(x.getInt(n))}
   o.optJSONArray("conditionalRules")?.let{x->for(n in 0 until x.length()){val q=x.getJSONObject(n);s.rules.add(Rule(q.optString("range"),q.optString("op"),q.optString("value"),q.optInt("bg",Color.rgb(22,77,49)),q.optInt("fg",Color.rgb(109,255,173))));}}
   o.optJSONObject("validationRules")?.let{x->for(k in x.keys()){val q=x.getJSONObject(k);val ar=q.optJSONArray("values")?:JSONArray();val vals=mutableListOf<String>();for(n in 0 until ar.length())vals.add(ar.getString(n));s.validations[k]=Validation(q.optString("type","text"),vals,if(q.has("min")&&!q.isNull("min"))q.optDouble("min") else null,if(q.has("max")&&!q.isNull("max"))q.optDouble("max") else null)}}
   ss.add(s)
  }
  if(ss.isEmpty())ss.add(Sheet(name="Planilha 1"));val id=j.optString("activeSheetId");val ai=ss.indexOfFirst{x->x.id==id};return Book(j.optString("_id").ifBlank{null},j.optString("name","Nova planilha"),ss,if(ai<0)0 else ai)
 }
 private fun req(path:String,method:String,body:String?,auth:String?):Pair<Int,String>{val c=URL(API+path).openConnection() as HttpURLConnection;c.requestMethod=method;c.connectTimeout=10000;c.readTimeout=15000;if(auth!=null)c.setRequestProperty("Authorization","Bearer "+auth);c.setRequestProperty("Content-Type","application/json");if(body!=null){c.doOutput=true;c.outputStream.use{it.write(body.toByteArray())}};val code=c.responseCode;val i=if(code>=400)c.errorStream else c.inputStream;return code to i.bufferedReader().use{it.readText()}}
 inner class Grid:View(this){var zoom=1f;var panX=0f;var panY=0f;var selStart=0;var selEnd=0;var selColStart=0;var selColEnd=0;val cw=130f;val rh=52f;val head=48f;val p=Paint(1)
   override fun onDraw(c:Canvas){
    val s=book!!.sheets[book!!.active];c.save();c.scale(zoom,zoom);p.textSize=14f
    fun visible(r:Int,k:Int):Boolean{val x=head+k*cw-if(k>=s.frozenCols)panX else 0f;val y=head+r*rh-if(r>=s.frozenRows)panY else 0f;return x+cw>=head&&x<=width/zoom&&y+rh>=head&&y<=height/zoom}
    for(r in 0 until 200)for(k in 0 until 50){
     if(s.hiddenRows.contains(r)||s.hiddenCols.contains(k)||!visible(r,k))continue
     val x=head+k*cw-if(k>=s.frozenCols)panX else 0f;val y=head+r*rh-if(r>=s.frozenRows)panY else 0f
     val ce=s.cells[key(r,k)];p.color=if(r in selStart..selEnd&&k in selColStart..selColEnd)Color.rgb(220,245,230)else Color.WHITE;c.drawRect(x,y,x+cw,y+rh,p)
     p.style=Paint.Style.STROKE;p.color=Color.LTGRAY;c.drawRect(x,y,x+cw,y+rh,p);p.style=Paint.Style.FILL
     if(ce!=null){val rule=s.rules.firstOrNull{ruleMatch(ce.input,key(r,k),it)};p.color=rule?.bg?:ce.background;c.drawRect(x+1,y+1,x+cw-1,y+rh-1,p)
      p.color=rule?.fg?:ce.fontColor;p.textSize=ce.fontSize;p.typeface=if(ce.bold&&ce.italic)Typeface.create(Typeface.DEFAULT,Typeface.BOLD_ITALIC)else if(ce.bold)Typeface.DEFAULT_BOLD else if(ce.italic)Typeface.create(Typeface.DEFAULT,Typeface.ITALIC)else Typeface.DEFAULT
      val tx=formatValue(showValue(ce.input,s),ce.numberFormat);val tw=p.measureText(tx);val txp=if(ce.align==1)x+(cw-tw)/2 else if(ce.align==2)x+cw-tw-7 else x+7;c.drawText(tx,txp,y+32,p);p.typeface=Typeface.DEFAULT
      if(ce.borderTop){p.style=Paint.Style.STROKE;p.color=Color.DKGRAY;c.drawLine(x,y,x+cw,y,p);p.style=Paint.Style.FILL};if(ce.borderRight){p.style=Paint.Style.STROKE;p.color=Color.DKGRAY;c.drawLine(x+cw,y,x+cw,y+rh,p);p.style=Paint.Style.FILL};if(ce.borderBottom){p.style=Paint.Style.STROKE;p.color=Color.DKGRAY;c.drawLine(x,y+rh,x+cw,y+rh,p);p.style=Paint.Style.FILL};if(ce.borderLeft){p.style=Paint.Style.STROKE;p.color=Color.DKGRAY;c.drawLine(x,y,x,y+rh,p);p.style=Paint.Style.FILL}
     }
    }
    p.color=Color.rgb(240,240,240);c.drawRect(0f,0f,width.toFloat(),head,p);var hx=head
    for(k in 0 until 50)if(!s.hiddenCols.contains(k)){val x=head+k*cw-if(k>=s.frozenCols)panX else 0f;if(x+cw>=head&&x<=width/zoom){p.color=Color.DKGRAY;p.textSize=14f;c.drawText(col(k),x+8,30f,p)}}
    for(r in 0 until 200)if(!s.hiddenRows.contains(r)){val y=head+r*rh-if(r>=s.frozenRows)panY else 0f;if(y+rh>=head&&y<=height/zoom){p.color=Color.DKGRAY;p.textSize=14f;c.drawText((r+1).toString(),8f,y+32,p)}}
    p.color=Color.rgb(14,28,21);c.drawRect(0f,0f,head,head,p);c.restore()
   }
   private fun ruleMatch(v:String,k:String,r:Rule):Boolean{
    if(!keyInRange(k,r.range))return false
    val n=v.replace(",",".").toDoubleOrNull()
    return when(r.op){
      "eq" -> v==r.value
      "neq" -> v!=r.value
      "contains" -> v.lowercase().contains(r.value.lowercase())
      "gt" -> n!=null && n>r.value.toDouble()
      "gte" -> n!=null && n>=r.value.toDouble()
      "lt" -> n!=null && n<r.value.toDouble()
      "lte" -> n!=null && n<=r.value.toDouble()
      else -> false
    }
   }
   private fun keyInRange(k:String,range:String):Boolean{
    val q=range.split(":");if(q.size!=2)return k==q[0]
    val a=parse(q[0]);val b=parse(q[1]);val p=parse(k)
    return p.first in minOf(a.first,b.first)..maxOf(a.first,b.first) && p.second in minOf(a.second,b.second)..maxOf(a.second,b.second)
   }
   private fun showValue(v:String,s:Sheet):String{
    if(!v.startsWith("=")){val n=v.replace(",",".").toDoubleOrNull();return if(n!=null&&s.cells.values.any{it.input==v&&it.numberFormat!="general"})n.toString()else v}
    return try{formatValue(eval(v.substring(1),s,mutableSetOf()),s.cells.values.firstOrNull{it.input==v}?.numberFormat?:"general")}catch(_:Exception){"#ERROR!"}
   }
   private fun eval(e0:String,s:Sheet,seen:MutableSet<String>):String{try{return FormulaParser(e0,s,seen).parse()}catch(e:Exception){return if(e.message=="CIRCULAR")"#CIRC!" else "#ERROR!"}}
   private fun formatValue(v:String,format:String):String{val n=v.replace(",",".").toDoubleOrNull()?:return v;return when(format){"currency"->"R$ "+String.format(java.util.Locale("pt","BR"),"%.2f",n);"percent"->String.format(java.util.Locale("pt","BR"),"%.2f%%",n*100);"number"->String.format(java.util.Locale("pt","BR"),"%.2f",n);"date"->try{SimpleDateFormat("dd/MM/yyyy").format(Date(n.toLong()))}catch(_:Exception){v};"time"->try{SimpleDateFormat("HH:mm:ss").format(Date(n.toLong()))}catch(_:Exception){v};else->v}}
   private inner class FormulaParser(private val src0:String,private val sheet:Sheet,private val seen:MutableSet<String>){
    private val src=src0.trim();private var pos=0
    private fun skip(){while(pos<src.length&&src[pos].isWhitespace())pos++}
    private fun peek(ch:Char)=run{skip();pos<src.length&&src[pos]==ch}
    private fun eat(ch:Char):Boolean{skip();if(pos<src.length&&src[pos]==ch){pos++;return true};return false}
    fun parse():String{val v=expr();skip();if(pos!=src.length)throw Exception("syntax");return v.toString()}
    private fun expr():Double{var v=term();while(true){if(eat('+'))v+=term()else if(eat('-'))v-=term()else return v}}
    private fun term():Double{var v=power();while(true){if(eat('*'))v*=power()else if(eat('/')){val d=power();if(d==0.0)throw Exception("DIV0");v/=d}else return v}}
    private fun power():Double{var v=unary();if(eat('^'))v=Math.pow(v,power());return v}
    private fun unary():Double{if(eat('+'))return unary();if(eat('-'))return -unary();return primary()}
    private fun primary():Double{
     skip();if(eat('(')){val v=expr();if(!eat(')'))throw Exception("paren");return v}
     val start=pos;while(pos<src.length&&!src[pos].isWhitespace()&&!"+-*/%^(),".contains(src[pos]))pos++
     if(start==pos)throw Exception("token");val token=src.substring(start,pos)
     if(token.replace(",",".").toDoubleOrNull()!=null)return token.replace(",",".").toDouble()
     skip()
     if(pos<src.length&&src[pos]=='('){
      pos++;val args=mutableListOf<String>();var depth=0;var last=pos
      while(pos<src.length){when(src[pos]){'('->{depth++};')'->{if(depth==0){if(pos>last)args.add(src.substring(last,pos));pos++;break}else depth--};','->{if(depth==0){args.add(src.substring(last,pos));last=pos+1}}};pos++}
      val fn=token.uppercase();if(fn !in setOf("SUM","AVERAGE","MIN","MAX","COUNT"))throw Exception("function")
      val vals=args.flatMap{argumentValues(it)};val nums=vals.mapNotNull{it.replace(",",".").toDoubleOrNull()}
      return when(fn){"SUM"->nums.sum();"AVERAGE"->if(nums.isEmpty())0.0 else nums.average();"MIN"->nums.minOrNull()?:0.0;"MAX"->nums.maxOrNull()?:0.0;"COUNT"->vals.count{it.replace(",",".").toDoubleOrNull()!=null}.toDouble();else->0.0}
     }
     return resolve(token)
    }
    private fun argumentValues(arg:String):List<String>{val t=arg.trim();val range=t.split(":");if(range.size==2){val a=cellPoint(range[0]);val b=cellPoint(range[1]);val out=mutableListOf<String>();for(r in minOf(a.first,b.first)..maxOf(a.first,b.first))for(c in minOf(a.second,b.second)..maxOf(a.second,b.second))out.add(resolveRaw(sheet,key(r,c)));return out};return listOf(resolveRaw(sheet,t))}
    private fun resolveRaw(s:Sheet,ref:String):String{val m=Regex("^(?:'((?:[^']|'')+)'|([A-Za-z0-9_ .-]+))!([A-Z]+[0-9]+)$").find(ref);if(m!=null){val name=(m.groupValues[1].ifBlank{m.groupValues[2]}).replace("''","'");val ts=book!!.sheets.firstOrNull{x->x.name==name}?:return "0";return resolveCell(ts,m.groupValues[3].uppercase())};return if(Regex("^[A-Z]+[1-9][0-9]*$",RegexOption.IGNORE_CASE).matches(ref))resolveCell(s,ref.uppercase()) else ref}
    private fun resolve(ref:String):Double{val raw=resolveRaw(sheet,ref);return raw.replace(",",".").toDoubleOrNull()?:throw Exception("number")}
    private fun resolveCell(s:Sheet,k:String):String{val id=s.id+"!"+k;if(!seen.add(id))throw Exception("CIRCULAR");try{val raw=s.cells[k]?.input?:"0";return if(raw.startsWith("="))eval(raw.substring(1),s,seen)else raw}finally{seen.remove(id)}}
    private fun cellPoint(k:String):Pair<Int,Int>{val m=Regex("([A-Z]+)([0-9]+)",RegexOption.IGNORE_CASE).find(k.trim())?:throw Exception("ref");var n=0;for(ch in m.groupValues[1].uppercase())n=n*26+ch.code-64;return Pair(m.groupValues[2].toInt()-1,n-1)}
   }
  private fun parse(x:String):Pair<Int,Int>{val m=Regex("([A-Z]+)([0-9]+)",RegexOption.IGNORE_CASE).find(x.trim())?:throw Exception();var n=0;for(ch in m.groupValues[1].uppercase())n=n*26+ch.code-64;return Pair(m.groupValues[2].toInt()-1,n-1)}
  private var lastX=0f;private var lastY=0f;private var panning=false
  override fun onTouchEvent(e:MotionEvent):Boolean{val x=e.x/zoom;val y=e.y/zoom;when(e.action){MotionEvent.ACTION_DOWN->{lastX=x;lastY=y;panning=false;selStart=locR(y);selEnd=selStart;selColStart=locC(x);selColEnd=selColStart;invalidate();return true};MotionEvent.ACTION_MOVE->{if(e.pointerCount>=2){panning=true;panX=(panX-(x-lastX)).coerceAtLeast(0f);panY=(panY-(y-lastY)).coerceAtLeast(0f);lastX=x;lastY=y;invalidate();return true};selEnd=locR(y);selColEnd=locC(x);invalidate();return true};MotionEvent.ACTION_UP->{if(!panning&&y>=head)edit(selStart,selColStart);return true}};return true}
  private fun locR(y:Float):Int{val s=book!!.sheets[book!!.active];val logical=if(y<head+s.frozenRows*rh)y-head else y-head+panY;return (logical/rh).toInt().coerceIn(0,199)}
  private fun locC(x:Float):Int{val s=book!!.sheets[book!!.active];val logical=if(x<head+s.frozenCols*cw)x-head else x-head+panX;return (logical/cw).toInt().coerceIn(0,49)}
 }
}