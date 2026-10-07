package com.korczaktech.nexa
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import android.app.*;import android.os.*;import android.content.*;import android.net.Uri;import android.graphics.*;import android.view.*;import android.widget.*;import org.json.*;import java.net.*;import java.util.*;import java.io.*;import java.text.SimpleDateFormat
private const val API="https://kz-nexa.onrender.com"
private data class Cell(var input:String="",var bold:Boolean=false,var italic:Boolean=false,var underline:Boolean=false,var strike:Boolean=false,var align:Int=0,var numberFormat:String="general",var fontSize:Float=14f,var fontColor:Int=Color.DKGRAY,var background:Int=Color.WHITE,var borderTop:Boolean=false,var borderRight:Boolean=false,var borderBottom:Boolean=false,var borderLeft:Boolean=false,var wrap:Boolean=false,var comment:String="")
private data class Rule(var range:String,var op:String,var value:String,var bg:Int=Color.rgb(22,77,49),var fg:Int=Color.rgb(109,255,173))
private data class Validation(var type:String,var values:List<String> = emptyList(),var min:Double?=null,var max:Double?=null)
private data class Sheet(val id:String=UUID.randomUUID().toString(),var name:String,var cells:MutableMap<String,Cell> = mutableMapOf(),var frozenRows:Int=0,var frozenCols:Int=0,var hiddenRows:MutableSet<Int> = mutableSetOf(),var hiddenCols:MutableSet<Int> = mutableSetOf(),var columnWidths:MutableMap<Int,Float> = mutableMapOf(),var rowHeights:MutableMap<Int,Float> = mutableMapOf(),var merged:MutableSet<String> = mutableSetOf(),var rules:MutableList<Rule> = mutableListOf(),var validations:MutableMap<String,Validation> = mutableMapOf(),var groupedRows:MutableSet<Int> = mutableSetOf(),var groupedCols:MutableSet<Int> = mutableSetOf(),var rowGroups:MutableList<String> = mutableListOf(),var colGroups:MutableList<String> = mutableListOf())
private data class Book(var id:String?=null,var name:String="Nova planilha",var sheets:MutableList<Sheet>,var active:Int=0)
class MainActivity:Activity(){private var aboutApiOnline=false
 private var currentAboutApiVersionView:TextView?=null
 private var currentAboutApiStatusView:TextView?=null
private fun checkAboutStatus(updateView:TextView){updateView.text="Verificando…";Thread{var available=false;var apiOnline=false;var apiVersion="Indisponível";try{val conn=URL("$API/health").openConnection() as HttpURLConnection;conn.connectTimeout=8000;conn.readTimeout=10000;conn.setRequestProperty("Accept","application/json");val code=conn.responseCode;if(code in 200..299){apiOnline=true;val j=JSONObject(conn.inputStream.bufferedReader().use{it.readText()});apiVersion="v"+j.optString("version","1.0.0")};conn.disconnect()}catch(_:Exception){};try{val conn=URL("https://api.github.com/repos/korczaktech/kz-nexa/releases?per_page=30").openConnection() as HttpURLConnection;conn.connectTimeout=8000;conn.readTimeout=10000;conn.setRequestProperty("Accept","application/vnd.github+json");val code=conn.responseCode;if(code in 200..299){val arr=JSONArray(conn.inputStream.bufferedReader().use{it.readText()});for(i in 0 until arr.length()){val q=arr.getJSONObject(i);if(!q.optBoolean("draft")&&!q.optBoolean("prerelease")&&isNewer(q.optString("tag_name").removePrefix("v"),APP_VERSION)){available=true;break}}};conn.disconnect()}catch(_:Exception){};runOnUiThread{updateView.text=if(available)"Disponível" else "Indisponível";aboutApiOnline=apiOnline;currentAboutApiVersionView?.text=apiVersion;currentAboutApiStatusView?.text=if(apiOnline)"On-Line" else "Off-Line"}}.start()}

 // stable startup path
 private var token:String?=null;private var uid="";private var profileName="Meu perfil";private var book:Book?=null;private val PICK=91;private val SAVE=92;private lateinit var root:FrameLayout;private lateinit var grid:Grid;private val APP_VERSION=BuildConfig.VERSION_NAME;private val APP_VERSION_CODE=BuildConfig.VERSION_CODE
 private var appReady=false
 private var isDarkTheme=false
 private var restoringPage=false
 private data class PageTarget(val render:(()->Unit)?)
 private var currentPageTarget:PageTarget?=null
 private val pageHistory=ArrayDeque<PageTarget>()
 override fun onBackPressed(){if(android.os.Build.VERSION.SDK_INT<33)handleBackNavigation()}
 private fun handleBackNavigation(){if(pageHistory.isNotEmpty()){val target=pageHistory.removeLast();restoringPage=true;try{if(target.render==null)home()else target.render.invoke()}finally{restoringPage=false}}else if(token!=null)confirmExitApp()else super.onBackPressed()}
 private fun confirmExitApp(){nexaBuilder().setTitle("Sair do Nexa?").setMessage("Tem certeza que deseja sair do aplicativo?").setNegativeButton("Cancelar",null).setPositiveButton("Sair"){_,_->finishAndRemoveTask()}.show()}
 private fun pageBack(){if(currentPageTarget?.render!=null&&book!=null)home()else handleBackNavigation()}
 override fun onCreate(b:Bundle?){installSplashScreen();super.onCreate(b);window.setBackgroundDrawableResource(android.R.color.transparent);isDarkTheme=getPreferences(0).getBoolean("darkTheme",false);applySystemTheme();root=FrameLayout(this);root.setBackgroundColor(Color.rgb(1,9,5));setContentView(root);if(android.os.Build.VERSION.SDK_INT>=33)getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT){handleBackNavigation()};appReady=true;token=getPreferences(0).getString("token",null);uid=getPreferences(0).getString("uid","")?:"";profileName=getPreferences(0).getString("profileName","Meu perfil")?:"Meu perfil";if(token==null)login()else load();android.os.Handler(mainLooper).postDelayed({checkForUpdate()},5000)}
 override fun onResume(){super.onResume();applySystemTheme();window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);window.attributes=window.attributes.apply{alpha=1f};window.decorView.alpha=1f;if(appReady&&::root.isInitialized){android.os.Handler(mainLooper).postDelayed({finishPendingInstallIfPossible()},250);android.os.Handler(mainLooper).postDelayed({checkForUpdate()},550)}}
 private fun finishPendingInstallIfPossible(){val uri=pendingInstallUri?:return;if(android.os.Build.VERSION.SDK_INT>=26&&!packageManager.canRequestPackageInstalls())return;pendingInstallUri=null;launchApkInstaller(uri)}
 private var updateCheckRunning=false
 private var updateDialogShowing=false
 private var pendingInstallUri:Uri?=null
 private val updateHandler=Handler(Looper.getMainLooper())

 private fun applySystemTheme(){val bg=if(isDarkTheme)Color.rgb(8,14,11)else Color.rgb(245,247,244);val nav=if(isDarkTheme)Color.rgb(12,20,16)else Color.WHITE;window.statusBarColor=bg;window.navigationBarColor=nav;window.decorView.systemUiVisibility=if(isDarkTheme)0 else View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR}
 private fun pageBg()=if(isDarkTheme)Color.rgb(12,20,16)else Color.rgb(248,250,248)
 private fun surfaceColor()=if(isDarkTheme)Color.rgb(22,31,26)else Color.WHITE
 private fun surfaceBorder()=if(isDarkTheme)Color.rgb(49,67,57)else Color.rgb(225,233,228)
 private fun inkColor()=if(isDarkTheme)Color.rgb(239,248,242)else Color.rgb(24,39,31)
 private fun mutedColor()=if(isDarkTheme)Color.rgb(166,190,175)else Color.rgb(100,119,109)
 private fun checkForUpdate(){
  if(updateCheckRunning||isFinishing||isDestroyed)return
  updateCheckRunning=true
  Thread{
   var retry=false
   try{
    val conn=(URL("https://api.github.com/repos/korczaktech/kz-nexa/releases/latest").openConnection() as HttpURLConnection).apply{
     instanceFollowRedirects=true;requestMethod="GET";setRequestProperty("Accept","application/vnd.github+json");setRequestProperty("User-Agent","Korczak-Nexa-Updater/1.0");setRequestProperty("X-GitHub-Api-Version","2022-11-28");setRequestProperty("Cache-Control","no-cache");useCaches=false;connectTimeout=12000;readTimeout=18000
    }
    if(conn.responseCode !in 200..299)throw IOException("GitHub HTTP ${conn.responseCode}")
    val release=JSONObject(conn.inputStream.bufferedReader().use{it.readText()})
    conn.disconnect()
    if(release.optBoolean("draft")||release.optBoolean("prerelease"))return@Thread
    val tag=release.optString("tag_name").trim().removePrefix("v")
    if(tag.isBlank()||!isNewer(tag,APP_VERSION))return@Thread
    val assets=release.optJSONArray("assets")?:return@Thread
    var download=""
    for(j in 0 until assets.length()){
      val asset=assets.getJSONObject(j)
      if(asset.optString("state","uploaded")!="uploaded")continue
      val name=asset.optString("name")
      val url=asset.optString("browser_download_url").trim()
      if(name.equals("Korczak-HUB-Nexa-$tag.apk",true)&&url.isNotBlank()){download=url;break}
      if(download.isBlank()&&name.endsWith(".apk",true)&&url.isNotBlank())download=url
    }
    if(download.isBlank())return@Thread
    runOnUiThread{if(!isFinishing&&!isDestroyed)showUpdateDialog(tag,download)}
   }catch(_:Exception){retry=true}
   finally{updateCheckRunning=false;if(retry)updateHandler.postDelayed({checkForUpdate()},15000)}
  }.start()
 } private fun parseVersion(value:String):IntArray?{
  val p=value.removePrefix("v").split(".")
  if(p.isEmpty()||p.any{it.toIntOrNull()==null})return null
  return IntArray(maxOf(4,p.size)){i->if(i<p.size)p[i].toInt()else 0}
 }
 private fun compareVersion(a:IntArray,b:IntArray):Int{
  for(i in 0 until maxOf(a.size,b.size)){val x=if(i<a.size)a[i]else 0;val y=if(i<b.size)b[i]else 0;if(x!=y)return x.compareTo(y)}
  return 0
 }
 private fun showUpdateDialog(tag:String,downloadUrl:String){
  if(updateDialogShowing||isFinishing||isDestroyed)return
  updateDialogShowing=true
  val box=LinearLayout(this).apply{
   orientation=LinearLayout.VERTICAL
   setPadding(dp(24),dp(22),dp(24),dp(18))
   background=rounded(Color.rgb(4,18,11),Color.rgb(64,190,113),26f)
  }
  val icon=TextView(this).apply{
   text="↻";gravity=Gravity.CENTER;textSize=27f;typeface=Typeface.DEFAULT_BOLD
   setTextColor(Color.rgb(116,255,166));background=rounded(Color.rgb(9,48,28),Color.rgb(64,190,113),18f)
  }
  box.addView(icon,LinearLayout.LayoutParams(dp(56),dp(56)).apply{gravity=Gravity.CENTER_HORIZONTAL;bottomMargin=dp(14)})
  val title=textView("Atualização disponível",21f,Color.WHITE).apply{gravity=Gravity.CENTER;typeface=Typeface.DEFAULT_BOLD}
  box.addView(title,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(7)})
  val msg=textView("Uma nova versão do Nexa está pronta.\n\nAtual: $APP_VERSION\nNova: $tag",14f,Color.rgb(190,222,204)).apply{gravity=Gravity.CENTER}
  box.addView(msg,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(20)})
  val actions=LinearLayout(this).apply{gravity=Gravity.CENTER}
  val later=textView("Agora não",14f,Color.rgb(181,215,195)).apply{gravity=Gravity.CENTER;typeface=Typeface.DEFAULT_BOLD;isClickable=true;background=rounded(Color.rgb(8,30,19),Color.rgb(55,100,74),15f)}
  val update=textView("Atualizar agora",14f,Color.rgb(2,20,11)).apply{gravity=Gravity.CENTER;typeface=Typeface.DEFAULT_BOLD;isClickable=true;background=rounded(Color.rgb(78,230,137),Color.rgb(142,255,183),15f)}
  actions.addView(later,LinearLayout.LayoutParams(0,dp(50),1f).apply{rightMargin=dp(6)})
  actions.addView(update,LinearLayout.LayoutParams(0,dp(50),1f).apply{leftMargin=dp(6)})
  box.addView(actions)
  val dialog=nexaBuilder().setView(box).setCancelable(false).create()
  later.setOnClickListener{dialog.dismiss()}
  update.setOnClickListener{dialog.dismiss();downloadUpdate(downloadUrl,tag)}
  dialog.setOnDismissListener{updateDialogShowing=false}
  dialog.setOnShowListener{
   dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
   dialog.window?.setDimAmount(.30f)
   dialog.window?.attributes=dialog.window?.attributes?.apply{dimAmount=.30f}
  }
  dialog.show()
 }
 private fun isNewer(remote:String,current:String):Boolean{val a=parseVersion(remote)?:return false;val b=parseVersion(current)?:return true;return compareVersion(a,b)>0}
 private fun downloadUpdate(url:String,tag:String){
  showUpdateToast("Baixando Nexa $tag...",false)
  Thread{
   var target:File?=null
   try{
    val updates=File(filesDir,"updates").apply{mkdirs()}
    target=File(updates,"Korczak-HUB-Nexa-$tag.apk")
    if(target.exists())target.delete()
    val conn=(URL(url).openConnection() as HttpURLConnection).apply{
     instanceFollowRedirects=true;requestMethod="GET"
     setRequestProperty("User-Agent","Korczak-Nexa-Updater")
     setRequestProperty("Accept","application/vnd.android.package-archive")
     connectTimeout=20000;readTimeout=30000
    }
    if(conn.responseCode !in 200..299)throw IOException("GitHub HTTP ${conn.responseCode}")
    conn.inputStream.use{input->FileOutputStream(target).use{output->
     val buffer=ByteArray(64*1024);var n:Int
     while(input.read(buffer).also{n=it}!=-1)output.write(buffer,0,n)
    }}
    conn.disconnect()
    if(!target.exists()||target.length()<100_000L)throw IOException("APK inválido ou incompleto")
    java.io.FileInputStream(target).use{input->
     val magic=ByteArray(4)
     if(input.read(magic)!=4||magic[0].toInt()!=0x50||magic[1].toInt()!=0x4B||magic[2].toInt()!=0x03||magic[3].toInt()!=0x04)throw IOException("O download não retornou um APK válido")
    }
    runOnUiThread{
     try{
      val file=target ?: throw IOException("Arquivo da atualização não encontrado")
      val uri=androidx.core.content.FileProvider.getUriForFile(this@MainActivity,"${BuildConfig.APPLICATION_ID}.fileprovider",file)
      if(android.os.Build.VERSION.SDK_INT>=26&&!packageManager.canRequestPackageInstalls()){
       pendingInstallUri=uri
       nexaBuilder()
        .setTitle("Permitir atualização do Nexa")
        .setMessage("O Android bloqueou a instalação automática. Ative “Permitir desta fonte” para o Nexa e volte ao aplicativo. A instalação continuará automaticamente.")
        .setPositiveButton("Abrir configuração"){_,_->try{startActivity(Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:$packageName")))}catch(_:Exception){try{startActivity(Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS))}catch(_:Exception){}}}
        .setNegativeButton("Agora não"){_,_->pendingInstallUri=null}.show()
      }else launchApkInstaller(uri)
     }catch(e:Exception){
      target?.delete();pendingInstallUri=null
      showUpdateToast("Não foi possível preparar a instalação: ${e.message ?: "arquivo inválido"}.",true)
     }
    }
   }catch(e:Exception){
    target?.delete()
    runOnUiThread{showUpdateToast("Falha na atualização: ${e.message ?: "arquivo inválido"}.",true)}
   }
  }.start()
 }
 private fun launchApkInstaller(uri:Uri){
  val grantFlags=Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_CLEAR_TOP
  try{
   val install=Intent(Intent.ACTION_INSTALL_PACKAGE).apply{
    setDataAndType(uri,"application/vnd.android.package-archive")
    addFlags(grantFlags)
    clipData=android.content.ClipData.newRawUri("Nexa APK",uri)
   }
   startActivity(install)
   return
  }catch(_:ActivityNotFoundException){}
   catch(_:SecurityException){}
   catch(_:Exception){}
  try{
   val view=Intent(Intent.ACTION_VIEW).apply{
    setDataAndType(uri,"application/vnd.android.package-archive")
    addFlags(grantFlags)
    clipData=android.content.ClipData.newRawUri("Nexa APK",uri)
   }
   startActivity(view)
  }catch(_:ActivityNotFoundException){
   showUpdateToast("O Android não encontrou o instalador de APK.",true)
  }catch(_:SecurityException){
   showUpdateToast("O Android bloqueou o acesso ao APK. Tente novamente.",true)
  }catch(_:Exception){
   showUpdateToast("O Android não conseguiu iniciar a instalação.",true)
  }
 }
 private enum class NexaToastType{SUCCESS,ERROR,WARNING,INFO,INPUT,SELECTION}
 private fun showNexaToast(message:String,type:NexaToastType=NexaToastType.INFO){
  val dark=isDarkTheme
  val fill=when(type){NexaToastType.SUCCESS->if(dark)Color.rgb(8,39,24)else Color.rgb(235,249,240);NexaToastType.ERROR->if(dark)Color.rgb(52,19,23)else Color.rgb(253,239,241);NexaToastType.WARNING->if(dark)Color.rgb(52,39,12)else Color.rgb(255,248,224);NexaToastType.INPUT->if(dark)Color.rgb(15,35,52)else Color.rgb(235,245,253);NexaToastType.SELECTION->if(dark)Color.rgb(31,28,55)else Color.rgb(242,239,255);else->if(dark)Color.rgb(20,31,25)else Color.WHITE}
  val stroke=when(type){NexaToastType.SUCCESS->if(dark)Color.rgb(74,224,130)else Color.rgb(47,169,94);NexaToastType.ERROR->if(dark)Color.rgb(229,91,105)else Color.rgb(204,65,82);NexaToastType.WARNING->if(dark)Color.rgb(235,190,67)else Color.rgb(210,164,39);NexaToastType.INPUT->if(dark)Color.rgb(82,169,232)else Color.rgb(66,139,202);NexaToastType.SELECTION->if(dark)Color.rgb(145,119,235)else Color.rgb(111,87,193);else->if(dark)Color.rgb(69,105,83)else Color.rgb(215,226,219)}
  val accent=when(type){NexaToastType.SUCCESS->if(dark)Color.rgb(105,255,155)else Color.rgb(18,116,70);NexaToastType.ERROR->if(dark)Color.rgb(255,150,160)else Color.rgb(171,38,55);NexaToastType.WARNING->if(dark)Color.rgb(255,219,111)else Color.rgb(145,105,13);NexaToastType.INPUT->if(dark)Color.rgb(124,202,255)else Color.rgb(37,105,166);NexaToastType.SELECTION->if(dark)Color.rgb(190,170,255)else Color.rgb(82,61,160);else->if(dark)Color.rgb(198,224,207)else Color.rgb(43,67,54)}
  val iconText=when(type){NexaToastType.SUCCESS->"✓";NexaToastType.ERROR->"×";NexaToastType.WARNING->"!";NexaToastType.INPUT->"⌨";NexaToastType.SELECTION->"☷";NexaToastType.INFO->"i"}
  val box=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(14),dp(9),dp(16),dp(9));background=rounded(fill,stroke,18f);elevation=dp(10).toFloat()}
  val icon=TextView(this).apply{text=iconText;gravity=Gravity.CENTER;textSize=17f;typeface=Typeface.DEFAULT_BOLD;setTextColor(accent);background=rounded(if(dark)Color.argb(35,255,255,255)else Color.argb(35,0,0,0),Color.TRANSPARENT,12f)}
  box.addView(icon,LinearLayout.LayoutParams(dp(32),dp(32)).apply{rightMargin=dp(10)})
  val tv=textView(message,13.5f,if(dark)Color.rgb(239,248,242) else Color.rgb(31,45,37));tv.typeface=Typeface.DEFAULT_BOLD;tv.maxLines=3;box.addView(tv,LinearLayout.LayoutParams(-2,-2))
  Toast(this).apply{duration=Toast.LENGTH_LONG;view=box;setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,0,dp(84));show()}
 }
 private fun showUpdateToast(message:String,error:Boolean){showNexaToast(message,if(error)NexaToastType.ERROR else NexaToastType.INFO)}
 private fun nexaBuilder():AlertDialog.Builder=AlertDialog.Builder(android.view.ContextThemeWrapper(this,if(isDarkTheme)R.style.NexaDialogDark else R.style.NexaDialogLight))
 private fun dialogInput(hint:String="",singleLine:Boolean=true):EditText{
  val e=EditText(this);e.hint=hint;e.setTextColor(inkColor());e.setHintTextColor(mutedColor());e.textSize=15f;e.setSingleLine(singleLine);e.setPadding(dp(14),dp(10),dp(14),dp(10));e.background=rounded(surfaceColor(),surfaceBorder(),14f)
  e.setOnFocusChangeListener{_,focused->e.background=rounded(surfaceColor(),if(focused)if(isDarkTheme)Color.rgb(82,232,139)else Color.rgb(19,122,84)else surfaceBorder(),14f)}
  return e
 }
 private fun dialogSpinner(items:Array<String>):Spinner{
  val s=Spinner(this);s.background=rounded(surfaceColor(),surfaceBorder(),14f);s.setPadding(dp(8),0,dp(8),0)
  s.adapter=object:ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,items){
   override fun getView(position:Int,convertView:View?,parent:android.view.ViewGroup):View{
    val v=TextView(this@MainActivity);v.text=items[position];v.textSize=14f;v.setTextColor(inkColor());v.gravity=Gravity.CENTER_VERTICAL;v.setPadding(dp(12),dp(10),dp(12),dp(10));return v
   }
   override fun getDropDownView(position:Int,convertView:View?,parent:android.view.ViewGroup):View{
    val v=getView(position,convertView,parent);v.background=rounded(surfaceColor(),Color.TRANSPARENT,0f);return v
   }
  };return s
 }
 private fun dp(v:Int)=((v*resources.displayMetrics.density)+0.5f).toInt()
 private fun textView(text:String,size:Float,color:Int=Color.WHITE):TextView{val v=TextView(this);v.text=text;v.textSize=size;v.setTextColor(color);return v}
 private fun rounded(fill:Int,stroke:Int=Color.TRANSPARENT,radius:Float=18f):android.graphics.drawable.GradientDrawable=android.graphics.drawable.GradientDrawable().apply{setColor(fill);if(stroke!=Color.TRANSPARENT)setStroke(dp(1),stroke);cornerRadius=dp(radius.toInt()).toFloat()}
 private fun inputField(hint:String,password:Boolean=false):EditText{
  val e=EditText(this);e.hint=hint;e.setTextColor(Color.WHITE);e.setHintTextColor(Color.rgb(128,164,143));e.textSize=15f;e.setSingleLine(true)
  e.setPadding(dp(18),0,dp(18),0);e.background=rounded(Color.rgb(8,23,15),Color.rgb(43,104,70),16f)
  e.setOnFocusChangeListener{_,focused->e.background=rounded(Color.rgb(9,28,18),if(focused)Color.rgb(82,232,139)else Color.rgb(43,104,70),16f)}
  if(password)e.inputType=129
  return e
 }
 private fun sendFeedback(category:String,subject:String,message:String,button:TextView){if(subject.length<3||message.length<10){showNexaToast("Preencha o título e descreva melhor o feedback.",NexaToastType.INPUT);return};button.isEnabled=false;Thread{try{val body=JSONObject().put("category",category).put("subject",subject).put("message",message).put("appVersion",APP_VERSION).put("platform","android").toString();val r=req("/v1/feedback","POST",body,token);runOnUiThread{button.isEnabled=true;if(r.first in 200..299){showNexaToast("Feedback enviado com sucesso.",NexaToastType.SUCCESS);pageBack()}else showNexaToast("Não foi possível enviar o feedback.",NexaToastType.ERROR)}}catch(_:Exception){runOnUiThread{button.isEnabled=true;showNexaToast("Falha de conexão ao enviar o feedback.",NexaToastType.ERROR)}}}.start()}
 private fun actionButton(label:String):TextView{
  val b=textView(label,15.5f);b.gravity=Gravity.CENTER;b.typeface=Typeface.create(Typeface.DEFAULT,Typeface.BOLD)
  b.setTextColor(Color.rgb(2,18,10));b.setPadding(dp(18),dp(14),dp(18),dp(14));b.isClickable=true;b.isFocusable=true;b.elevation=dp(7).toFloat()
  val normal=rounded(Color.rgb(73,220,128),Color.rgb(129,255,170),17f)
  val pressed=rounded(Color.rgb(42,159,91),Color.rgb(96,224,140),17f)
  val disabled=rounded(Color.rgb(38,73,52),Color.rgb(57,104,75),17f)
  b.background=android.graphics.drawable.StateListDrawable().apply{addState(intArrayOf(android.R.attr.state_enabled,android.R.attr.state_pressed),pressed);addState(intArrayOf(-android.R.attr.state_enabled),disabled);addState(intArrayOf(),normal)}
  return b
 }
 private class NexaWordmarkView(context:android.content.Context):View(context){
 private val paintWhite=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply{style=Paint.Style.FILL;color=Color.WHITE;isDither=true}
 private val paintGreen=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply{style=Paint.Style.FILL;color=Color.rgb(32,169,104);isDither=true}
 private val p=Path()
 override fun onDraw(c:Canvas){
  val scale=0.54f;val sx=width/600f*scale;val sy=height/180f*scale;c.save();c.translate(width*(1f-scale)/2f,height*(1f-scale)/2f);c.scale(sx,sy);p.fillType=Path.FillType.EVEN_ODD
  p.reset();w0(p);c.drawPath(p,paintWhite)
  p.reset();w1(p);c.drawPath(p,paintWhite)
  p.reset();w2(p);c.drawPath(p,paintWhite)
  p.reset();g0(p);c.drawPath(p,paintGreen)
  p.reset();g1(p);c.drawPath(p,paintGreen)
  p.reset();g2(p);c.drawPath(p,paintGreen)
  c.restore()
 }
 private fun w0(p:Path){
p.moveTo(8f,1f);p.lineTo(8f,29f);p.lineTo(7f,30f);p.lineTo(7f,45f);p.lineTo(8f,46f);p.lineTo(7f,50f);p.lineTo(7f,69f);p.lineTo(8f,70f);p.lineTo(8f,117f);p.lineTo(7f,118f);p.lineTo(8f,168f);p.lineTo(43f,168f);p.lineTo(43f,67f);p.lineTo(44f,66f);p.lineTo(46f,66f);p.lineTo(84f,117f);p.lineTo(102f,139f);p.lineTo(106f,146f);p.lineTo(124f,168f);p.lineTo(154f,168f);p.lineTo(155f,165f);p.lineTo(155f,158f);p.lineTo(154f,157f);p.lineTo(154f,3f);p.lineTo(155f,2f);p.lineTo(154f,1f);p.lineTo(119f,1f);p.lineTo(119f,100f);p.lineTo(117f,102f);p.lineTo(40f,1f);p.close()
 }
 private fun w1(p:Path){
p.moveTo(215f,40f);p.lineTo(201f,47f);p.lineTo(190f,56f);p.lineTo(182f,66f);p.lineTo(175f,81f);p.lineTo(172f,96f);p.lineTo(172f,111f);p.lineTo(175f,125f);p.lineTo(181f,138f);p.lineTo(185f,144f);p.lineTo(200f,158f);p.lineTo(211f,164f);p.lineTo(224f,168f);p.lineTo(238f,169f);p.lineTo(239f,170f);p.lineTo(252f,170f);p.lineTo(253f,169f);p.lineTo(267f,168f);p.lineTo(283f,162f);p.lineTo(292f,156f);p.lineTo(299f,149f);p.lineTo(299f,147f);p.lineTo(281f,129f);p.lineTo(278f,130f);p.lineTo(274f,134f);p.lineTo(257f,141f);p.lineTo(250f,141f);p.lineTo(249f,142f);p.lineTo(236f,141f);p.lineTo(222f,135f);p.lineTo(217f,131f);p.lineTo(213f,126f);p.lineTo(208f,114f);p.lineTo(209f,113f);p.lineTo(308f,113f);p.lineTo(307f,86f);p.lineTo(303f,74f);p.lineTo(298f,64f);p.lineTo(292f,56f);p.lineTo(283f,48f);p.lineTo(264f,39f);p.lineTo(255f,37f);p.lineTo(237f,36f);p.lineTo(236f,37f);p.lineTo(228f,37f);p.close()
p.moveTo(208f,89f);p.lineTo(210f,83f);p.lineTo(216f,73f);p.lineTo(224f,67f);p.lineTo(233f,63f);p.lineTo(249f,63f);p.lineTo(259f,67f);p.lineTo(268f,75f);p.lineTo(274f,87f);p.lineTo(273f,92f);p.lineTo(210f,92f);p.close()
 }
 private fun w2(p:Path){
p.moveTo(309f,39f);p.lineTo(355f,99f);p.lineTo(356f,102f);p.lineTo(306f,168f);p.lineTo(346f,168f);p.lineTo(376f,128f);p.lineTo(380f,121f);p.lineTo(383f,119f);p.lineTo(387f,112f);p.lineTo(394f,104f);p.lineTo(396f,99f);p.lineTo(350f,39f);p.close()
 }
 private fun g0(p:Path){
p.moveTo(462f,50f);p.lineTo(461f,54f);p.lineTo(474f,76f);p.lineTo(487f,69f);p.lineTo(499f,65f);p.lineTo(520f,64f);p.lineTo(527f,66f);p.lineTo(534f,70f);p.lineTo(540f,77f);p.lineTo(543f,88f);p.lineTo(541f,90f);p.lineTo(500f,90f);p.lineTo(499f,91f);p.lineTo(493f,91f);p.lineTo(472f,98f);p.lineTo(463f,105f);p.lineTo(456f,117f);p.lineTo(454f,131f);p.lineTo(457f,145f);p.lineTo(460f,151f);p.lineTo(465f,157f);p.lineTo(476f,165f);p.lineTo(484f,168f);p.lineTo(493f,170f);p.lineTo(515f,170f);p.lineTo(533f,164f);p.lineTo(544f,154f);p.lineTo(546f,157f);p.lineTo(546f,168f);p.lineTo(578f,168f);p.lineTo(579f,167f);p.lineTo(579f,87f);p.lineTo(575f,67f);p.lineTo(571f,59f);p.lineTo(564f,50f);p.lineTo(549f,41f);p.lineTo(539f,38f);p.lineTo(527f,37f);p.lineTo(526f,36f);p.lineTo(506f,36f);p.lineTo(505f,37f);p.lineTo(493f,38f);p.lineTo(476f,43f);p.close()
p.moveTo(490f,130f);p.lineTo(492f,121f);p.lineTo(497f,116f);p.lineTo(507f,112f);p.lineTo(541f,112f);p.lineTo(543f,114f);p.lineTo(543f,123f);p.lineTo(540f,132f);p.lineTo(535f,138f);p.lineTo(524f,144f);p.lineTo(520f,145f);p.lineTo(502f,144f);p.lineTo(498f,142f);p.lineTo(492f,136f);p.close()
 }
 private fun g1(p:Path){
p.moveTo(475f,0f);p.lineTo(419f,0f);p.lineTo(426f,5f);p.lineTo(431f,11f);p.lineTo(406f,40f);p.lineTo(384f,68f);p.lineTo(384f,70f);p.lineTo(402f,94f);p.lineTo(454f,30f);p.lineTo(458f,31f);p.lineTo(471f,41f);p.close()
 }
 private fun g2(p:Path){
p.moveTo(403f,106f);p.lineTo(383f,132f);p.lineTo(383f,134f);p.lineTo(408f,168f);p.lineTo(449f,168f);p.close()
 }
}
private class AuthBackgroundView(context:android.content.Context):View(context){
  private val p=Paint(Paint.ANTI_ALIAS_FLAG)
  override fun onDraw(c:Canvas){
   val w=width.toFloat();val h=height.toFloat()
   val g=android.graphics.LinearGradient(0f,0f,w,h,intArrayOf(Color.rgb(1,9,5),Color.rgb(2,48,25),Color.rgb(4,30,17),Color.rgb(0,12,6)),null,Shader.TileMode.CLAMP)
   p.shader=g;c.drawRect(0f,0f,w,h,p);p.shader=null
   val glow=android.graphics.RadialGradient(w*0.78f,h*0.18f,w*0.55f,intArrayOf(Color.argb(150,54,225,126),Color.argb(20,30,150,78),Color.TRANSPARENT),null,Shader.TileMode.CLAMP)
   p.shader=glow;c.drawCircle(w*0.78f,h*0.18f,w*0.55f,p);p.shader=null
   val glow2=android.graphics.RadialGradient(w*0.12f,h*0.84f,w*0.5f,intArrayOf(Color.argb(95,45,190,105),Color.TRANSPARENT),null,Shader.TileMode.CLAMP)
   p.shader=glow2;c.drawCircle(w*0.12f,h*0.84f,w*0.5f,p);p.shader=null
   p.color=Color.argb(42,102,255,165);p.strokeWidth=1f
   val step=dpLocal(38);var x=0f;while(x<w){c.drawLine(x,0f,x,h,p);x+=step};var y=0f;while(y<h){c.drawLine(0f,y,w,y,p);y+=step}
   p.color=Color.argb(75,108,235,157);var dx=step*0.5f;while(dx<w){var dy=step*0.5f;while(dy<h){c.drawCircle(dx,dy,1.3f,p);dy+=step};dx+=step}
  }
  private fun dpLocal(v:Int)=v*resources.displayMetrics.density
 }
 private fun login(){root.setBackgroundColor(Color.rgb(1,9,5));showAuth("login")}
 private fun showAuth(mode:String){
  root.removeAllViews()
  val bg=AuthBackgroundView(this);root.addView(bg,FrameLayout.LayoutParams(-1,-1))
  val scroll=ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(Color.TRANSPARENT)
  val l=LinearLayout(this);l.orientation=LinearLayout.VERTICAL;l.gravity=Gravity.CENTER_HORIZONTAL;l.setPadding(dp(22),dp(42),dp(22),dp(30))
  val card=LinearLayout(this);card.orientation=LinearLayout.VERTICAL;card.setPadding(dp(24),dp(26),dp(24),dp(24))
  card.background=rounded(Color.rgb(6,20,13),Color.rgb(52,119,79),28f);card.elevation=0f
  val logo=ImageView(this);logo.setImageResource(R.drawable.nexa_login_logo);logo.scaleType=ImageView.ScaleType.CENTER_INSIDE
  card.addView(logo,LinearLayout.LayoutParams(dp(72),dp(72)).apply{gravity=Gravity.CENTER_HORIZONTAL;bottomMargin=dp(6)})
  val word=NexaWordmarkView(this);word.translationY=-dp(7).toFloat();card.addView(word,LinearLayout.LayoutParams(-1,dp(92)).apply{bottomMargin=dp(5)})
  val sub=textView(if(mode=="login")"Suas Planilhas. Sua organização. Seu Nexa." else if(mode=="register")"Crie seu acesso ao Nexa." else "Recupere o acesso ao seu Nexa.",13.5f,Color.rgb(145,190,161));sub.gravity=Gravity.CENTER
  card.addView(sub,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(25)})
  val title=textView(if(mode=="login")"Entrar" else if(mode=="register")"Criar conta" else "Recuperar acesso",22f);title.typeface=Typeface.DEFAULT_BOLD
  card.addView(title,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(14)})
  val email=inputField("Email");card.addView(email,LinearLayout.LayoutParams(-1,dp(56)).apply{bottomMargin=dp(11)})
  if(mode=="register"){
   val name=inputField("Nome completo");card.addView(name,LinearLayout.LayoutParams(-1,dp(56)).apply{bottomMargin=dp(11)})
   val pass=inputField("Senha",true);card.addView(pass,LinearLayout.LayoutParams(-1,dp(56)).apply{bottomMargin=dp(11)})
   val confirm=inputField("Confirmar senha",true);card.addView(confirm,LinearLayout.LayoutParams(-1,dp(56)).apply{bottomMargin=dp(14)})
   val msg=textView("",13f,Color.rgb(255,133,133));card.addView(msg,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(10)})
   val go=actionButton("Criar conta");card.addView(go,LinearLayout.LayoutParams(-1,dp(56)))
   go.setOnClickListener{
    if(name.text.toString().trim().isEmpty()||email.text.toString().trim().isEmpty()||pass.text.length<6){msg.text="Preencha os campos e use uma senha com pelo menos 6 caracteres.";return@setOnClickListener}
    if(pass.text.toString()!=confirm.text.toString()){msg.text="As senhas não coincidem.";return@setOnClickListener}
    go.isEnabled=false;go.text="Criando conta..."
    msg.setTextColor(Color.rgb(145,190,161));msg.text="Criando sua conta..."
    Handler(mainLooper).postDelayed({go.isEnabled=true;go.text="Criar conta";msg.text="Cadastro preparado. A ativação será concluída pelo serviço Nexa."},700)
   }
  }else if(mode=="recover"){
   val msg=textView("Informe seu email para receber as instruções de recuperação.",13f,Color.rgb(145,190,161));card.addView(msg,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(16)})
   val go=actionButton("Enviar instruções");card.addView(go,LinearLayout.LayoutParams(-1,dp(56)))
   go.setOnClickListener{if(email.text.toString().trim().isEmpty()){msg.setTextColor(Color.rgb(255,133,133));msg.text="Informe seu email.";return@setOnClickListener};go.isEnabled=false;go.text="Enviando...";msg.setTextColor(Color.rgb(145,190,161));msg.text="Enviando instruções...";Handler(mainLooper).postDelayed({go.isEnabled=true;go.text="Enviar instruções";msg.text="Se o email estiver cadastrado, você receberá as instruções."},900)}
  }else{
   val pass=inputField("Senha",true);card.addView(pass,LinearLayout.LayoutParams(-1,dp(56)).apply{bottomMargin=dp(11)})
   val err=textView("",13f,Color.rgb(255,133,133));card.addView(err,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(10)})
   val bt=actionButton("Entrar");card.addView(bt,LinearLayout.LayoutParams(-1,dp(56)))
   bt.setOnClickListener{
    if(email.text.toString().trim().isEmpty()||pass.text.toString().isEmpty()){err.text="Informe email e senha.";return@setOnClickListener}
    bt.isEnabled=false;bt.text="Entrando...";err.text=""
    Thread{try{val r=req("/v1/auth/login","POST",JSONObject().put("email",email.text.toString()).put("password",pass.text.toString()).toString(),null);if(r.first !in 200..299)throw Exception(JSONObject(r.second).optString("message","Não foi possível entrar."));val j=JSONObject(r.second);token=j.getString("token");val user=j.getJSONObject("user");uid=user.getString("id");profileName=user.optString("name",user.optString("email","Meu perfil")).ifBlank{"Meu perfil"};getPreferences(0).edit().putString("token",token).putString("uid",uid).putString("profileName",profileName).putString("email",user.optString("email","")).apply();runOnUiThread{loadHomeAfterLogin()}}catch(x:Exception){runOnUiThread{err.text=x.message?:"Falha ao entrar";bt.isEnabled=true;bt.text="Entrar"}}}.start()
   }
  }
  val links=LinearLayout(this);links.gravity=Gravity.CENTER;links.setPadding(0,dp(17),0,0)
  fun link(label:String,next:String){val b=textView(label,14f,Color.rgb(90,230,142));b.setPadding(dp(9),dp(9),dp(9),dp(9));b.isClickable=true;b.setOnClickListener{showAuth(next)};links.addView(b)}
  if(mode!="login")link("Entrar","login")
  if(mode=="login"){link("Criar conta","register");link("Recuperar acesso","recover")}else if(mode!="recover"){link("Recuperar acesso","recover")}
  card.addView(links)
  l.addView(card,LinearLayout.LayoutParams(-1,-2).apply{gravity=Gravity.CENTER_HORIZONTAL})
  val footer=textView("KORCZAK HUB",11.5f,Color.rgb(108,220,148));footer.gravity=Gravity.CENTER;footer.typeface=Typeface.create(Typeface.DEFAULT,Typeface.BOLD);if(android.os.Build.VERSION.SDK_INT>=21)footer.letterSpacing=0.22f;footer.setPadding(0,dp(8),0,dp(8));val footerWrap=LinearLayout(this);footerWrap.gravity=Gravity.CENTER;val line=View(this);line.setBackgroundColor(Color.rgb(43,116,73));footerWrap.addView(line,LinearLayout.LayoutParams(dp(34),dp(1)).apply{rightMargin=dp(10)});footerWrap.addView(footer,LinearLayout.LayoutParams(-2,-2));val line2=View(this);line2.setBackgroundColor(Color.rgb(43,116,73));footerWrap.addView(line2,LinearLayout.LayoutParams(dp(34),dp(1)).apply{leftMargin=dp(10)});l.addView(footerWrap,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(17)})
  scroll.addView(l);root.addView(scroll,FrameLayout.LayoutParams(-1,-1))
 }
 private fun load(){Thread{try{val r=req("/v1/workbooks","GET",null,token);val a=JSONArray(r.second);book=if(a.length()>0)from(a.getJSONObject(0))else Book(sheets=mutableListOf(Sheet(name="Planilha 1")));runOnUiThread{home()}}catch(x:Exception){runOnUiThread{showNexaToast("Falha ao carregar",NexaToastType.ERROR);login()}}}.start()}
 private fun loadHomeAfterLogin(){load()}
 private fun showMatrixSplash(done:()->Unit){runOnUiThread{try{done()}catch(_:Throwable){login()}}}
 private inner class MatrixSplashView(context:Context):View(context){
  private val paint=Paint(Paint.ANTI_ALIAS_FLAG);private val chars="01NEXA";private val random=java.util.Random();private val columns=mutableListOf<Float>();private var running=true;private val ticker=object:Runnable{override fun run(){if(!running)return;invalidate();postDelayed(this,55)}}
  init{setBackgroundColor(Color.rgb(1,9,5));post(ticker)}
  override fun onDraw(c:Canvas){super.onDraw(c);if(columns.isEmpty()){val count=(width/22f).toInt().coerceAtLeast(1);repeat(count){columns.add(random.nextFloat()*-height)}};paint.typeface=Typeface.MONOSPACE;paint.textSize=15f;for(i in columns.indices){val x=i*22f;var y=columns[i];repeat(7){val ch=chars[random.nextInt(chars.length)].toString();paint.alpha=(255-it*28).coerceAtLeast(35);paint.color=Color.rgb(70,220,125);c.drawText(ch,x,y,paint);y+=18f};columns[i]+=12f;if(columns[i]>height+120)columns[i]=random.nextFloat()*-height};paint.alpha=255;paint.textAlign=Paint.Align.CENTER;paint.typeface=Typeface.create(Typeface.DEFAULT,Typeface.BOLD);paint.textSize=42f;paint.color=Color.WHITE;c.drawText("Nexa",width/2f,height/2f-8f,paint);paint.textSize=13f;paint.color=Color.rgb(108,220,148);c.drawText("INICIALIZANDO",width/2f,height/2f+28f,paint);paint.textAlign=Paint.Align.LEFT}
  fun stop(){running=false;removeCallbacks(ticker)}
 }
 private fun home(){
  try{
   currentPageTarget=null
   root.removeAllViews()
   applySystemTheme()
   window.decorView.systemUiVisibility=if(isDarkTheme)0 else View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
   
   val view=layoutInflater.inflate(R.layout.activity_main,root,false)
   root.addView(view)
   val name=profileName.ifBlank{"Nexa"}
   val initials=name.trim().split(Regex("\\s+")).filter{it.isNotEmpty()}.take(2).joinToString(""){it.first().uppercase()}.ifBlank{"N"}
   view.findViewById<TextView>(R.id.userName).text=name
   view.findViewById<TextView>(R.id.avatar).text=initials
   view.findViewById<TextView>(R.id.featuredName).text=book?.name?.ifBlank{"Nova planilha"}?:"Nova planilha"
   view.findViewById<TextView>(R.id.featuredEdited).text="Editado recentemente"
   val preview=view.findViewById<GridLayout>(R.id.gridPreview)
   preview.removeAllViews()
   val highlighted=setOf(1,4,9,10,17,19)
   for(i in 0 until 24){
    val cell=View(this)
    cell.setBackgroundResource(if(i==11)R.drawable.bg_cell_sel else if(i in highlighted)R.drawable.bg_cell_hl else R.drawable.bg_cell)
    val lp=GridLayout.LayoutParams(GridLayout.spec(i/8),GridLayout.spec(i%8,1f))
    lp.width=0;lp.height=dp(18);lp.setMargins(dp(2),dp(2),dp(2),dp(2))
    preview.addView(cell,lp)
   }
   val list=view.findViewById<LinearLayout>(R.id.listRecents)
   list.removeAllViews()
   val recentBook=book?.name?.ifBlank{"Nova planilha"}?:"Nova planilha"
   val row=layoutInflater.inflate(R.layout.item_recent,list,false)
   row.findViewById<TextView>(R.id.recentName).text=recentBook
   row.findViewById<TextView>(R.id.recentMeta).text="Editado recentemente"
   row.setOnClickListener{editor()};list.addView(row)
   view.findViewById<View>(R.id.btnRefresh).setOnClickListener{it.animate().rotationBy(360f).setDuration(600).start();load()}
   view.findViewById<View>(R.id.avatar).setOnClickListener{profilePage()}
   view.findViewById<View>(R.id.userName).setOnClickListener{profilePage()}
   view.findViewById<View>(R.id.btnOpen).setOnClickListener{editor()}
   view.findViewById<View>(R.id.btnSeeAll).setOnClickListener{allDocumentsPage()}
   view.findViewById<View>(R.id.shortcutImport).setOnClickListener{addFilePage()}
   view.findViewById<View>(R.id.shortcutOpen).setOnClickListener{filesPage()}
   view.findViewById<View>(R.id.shortcutFavorites).setOnClickListener{favoritesPage()}
   view.findViewById<View>(R.id.navHome).setOnClickListener{home()}
   view.findViewById<View>(R.id.navFiles).setOnClickListener{filesPage()}
   view.findViewById<View>(R.id.btnCreate).setOnClickListener{newDocument()}
   view.findViewById<View>(R.id.navTemplates).setOnClickListener{templatesPage()};view.findViewById<View>(R.id.iconTemplates).setOnClickListener{templatesPage()};view.findViewById<View>(R.id.labelTemplates).setOnClickListener{templatesPage()}
   view.findViewById<View>(R.id.navMore).setOnClickListener{morePage()}
   val green=Color.rgb(19,122,84);val muted=Color.rgb(91,106,98)
   view.findViewById<View>(R.id.pillHome).visibility=View.VISIBLE
   view.findViewById<View>(R.id.pillFiles).visibility=View.INVISIBLE
   view.findViewById<View>(R.id.pillTemplates).visibility=View.INVISIBLE
   view.findViewById<View>(R.id.pillMore).visibility=View.INVISIBLE
   view.findViewById<ImageView>(R.id.iconHome).setColorFilter(green)
   view.findViewById<ImageView>(R.id.iconFiles).setColorFilter(muted)
   view.findViewById<ImageView>(R.id.iconTemplates).setColorFilter(muted)
   view.findViewById<ImageView>(R.id.iconMore).setColorFilter(muted)
   view.findViewById<TextView>(R.id.labelHome).setTextColor(green)
   view.findViewById<TextView>(R.id.labelFiles).setTextColor(muted)
   view.findViewById<TextView>(R.id.labelTemplates).setTextColor(muted)
   view.findViewById<TextView>(R.id.labelMore).setTextColor(muted)
   if(isDarkTheme) applyHomeDarkTheme(view)
  }catch(x:Throwable){android.util.Log.e("Nexa","Home initialization failed",x);showHomeFailure(x)}
 }
 private fun applyHomeDarkTheme(view:View){
  val bg=Color.rgb(12,20,16);val surface=Color.rgb(22,31,26);val border=Color.rgb(49,67,57);val ink=Color.rgb(239,248,242);val muted=Color.rgb(166,190,175);val green=Color.rgb(82,232,139)
  view.setBackgroundColor(bg)
  view.findViewById<View>(R.id.header).setBackgroundColor(bg)
  view.findViewById<View>(R.id.scroll).setBackgroundColor(bg)
  val scroll=view.findViewById<androidx.core.widget.NestedScrollView>(R.id.scroll)
  val content=scroll.getChildAt(0)
  content?.setBackgroundColor(bg)
  val contentGroup=content as? ViewGroup
  content?.findViewById<View>(R.id.search)?.background=rounded(surface,border,14f)
  content?.findViewById<TextView>(R.id.featuredName)?.setTextColor(Color.WHITE)
  content?.findViewById<TextView>(R.id.featuredEdited)?.setTextColor(Color.rgb(188,235,208))
  content?.findViewById<TextView>(R.id.btnSeeAll)?.setTextColor(green)
  (contentGroup?.getChildAt(0) as? TextView)?.setTextColor(ink)
  listOf(R.id.shortcutImport,R.id.shortcutOpen,R.id.shortcutFavorites).forEach{id->content?.findViewById<View>(id)?.let{it.background=rounded(surface,border,16f)}}
  listOf(R.id.shortcutImport,R.id.shortcutOpen,R.id.shortcutFavorites).forEach{id->content?.findViewById<View>(id)?.let{box->if(box is ViewGroup)for(i in 0 until box.childCount){val child=box.getChildAt(i);if(child is TextView)child.setTextColor(ink);if(child is ImageView)child.setColorFilter(green)}}}
  val list=content?.findViewById<LinearLayout>(R.id.listRecents)
  list?.getChildAt(0)?.let{row->row.background=rounded(surface,border,14f);row.findViewById<TextView>(R.id.recentName)?.setTextColor(ink);row.findViewById<TextView>(R.id.recentMeta)?.setTextColor(muted);if(row is ViewGroup)for(i in 0 until row.childCount){val child=row.getChildAt(i);if(child is ImageView)child.setColorFilter(green)}}
  view.findViewById<View>(R.id.navBar).setBackgroundColor(Color.rgb(16,25,20))
  view.findViewById<View>(R.id.pillHome).background=rounded(Color.rgb(25,56,42),Color.TRANSPARENT,15f)
  view.findViewById<ImageView>(R.id.iconHome).setColorFilter(green);view.findViewById<ImageView>(R.id.iconFiles).setColorFilter(muted);view.findViewById<ImageView>(R.id.iconTemplates).setColorFilter(muted);view.findViewById<ImageView>(R.id.iconMore).setColorFilter(muted)
  view.findViewById<TextView>(R.id.labelHome).setTextColor(green);view.findViewById<TextView>(R.id.labelFiles).setTextColor(muted);view.findViewById<TextView>(R.id.labelTemplates).setTextColor(muted);view.findViewById<TextView>(R.id.labelMore).setTextColor(muted)
  view.findViewById<ImageView>(R.id.btnRefresh).setColorFilter(ink);view.findViewById<TextView>(R.id.userName).setTextColor(ink)
  view.findViewById<Button>(R.id.btnOpen).apply{background=rounded(surface,border,22f);setTextColor(green)}
  val nav=view.findViewById<View>(R.id.navBar);if(nav is ViewGroup&&nav.childCount>0)nav.getChildAt(0).setBackgroundColor(Color.rgb(37,53,44))
 }
 private fun showHomeFailure(x:Throwable){
  try{
   root.removeAllViews();window.statusBarColor=Color.rgb(1,9,5);window.navigationBarColor=Color.rgb(6,16,11)
   val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;setPadding(dp(28),dp(28),dp(28),dp(28));setBackgroundColor(Color.rgb(1,9,5))}
   box.addView(textView("Nexa",30f,Color.WHITE).apply{gravity=Gravity.CENTER})
   box.addView(textView("Não foi possível abrir a interface.",16f,Color.rgb(255,150,150)).apply{gravity=Gravity.CENTER;setPadding(0,dp(14),0,dp(8))})
   box.addView(textView("O acesso foi validado, mas a tela inicial encontrou um erro.\n\n"+x.javaClass.simpleName+": "+(x.message?:"sem detalhes"),13f,Color.rgb(190,220,201)).apply{gravity=Gravity.CENTER;textAlignment=View.TEXT_ALIGNMENT_CENTER})
   val retry=actionButton("Tentar novamente");retry.setOnClickListener{load()}
   box.addView(retry,LinearLayout.LayoutParams(-1,dp(54)).apply{topMargin=dp(22)})
   val logout=textView("Voltar ao login",14f,Color.rgb(100,235,150)).apply{gravity=Gravity.CENTER;isClickable=true}
   logout.setOnClickListener{getPreferences(0).edit().clear().apply();token=null;uid="";profileName="Meu perfil";book=null;login()}
   box.addView(logout,LinearLayout.LayoutParams(-1,dp(48)).apply{topMargin=dp(8)})
   root.addView(box,FrameLayout.LayoutParams(-1,-1))
  }catch(_:Throwable){try{getPreferences(0).edit().clear().apply();token=null;login()}catch(_:Throwable){}}
 }
 private fun storageCard():View{val box=LinearLayout(this);box.orientation=LinearLayout.VERTICAL;box.setPadding(dp(17),dp(15),dp(17),dp(14));box.background=rounded(Color.rgb(5,27,16),Color.rgb(37,108,67),20f);val stat=android.os.StatFs(android.os.Environment.getDataDirectory().path);val total=stat.totalBytes.toDouble();val free=stat.availableBytes.toDouble();val used=(total-free).coerceAtLeast(0.0);val pct=((used/total)*100.0).coerceIn(0.0,100.0);val row=LinearLayout(this);row.gravity=Gravity.CENTER_VERTICAL;val t=textView("Armazenamento do celular",15f,Color.WHITE);t.typeface=Typeface.DEFAULT_BOLD;row.addView(t,LinearLayout.LayoutParams(0,-2,1f));val p=textView(String.format(java.util.Locale("pt","BR"),"%.1f%%",pct),16f,Color.rgb(110,255,157));p.typeface=Typeface.DEFAULT_BOLD;row.addView(p);box.addView(row);val bar=android.widget.ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);bar.max=1000;bar.progress=(pct*10).toInt();bar.progressDrawable=android.graphics.drawable.ClipDrawable(rounded(Color.rgb(88,236,137),Color.TRANSPARENT,8f),Gravity.LEFT,1);box.addView(bar,LinearLayout.LayoutParams(-1,dp(9)).apply{topMargin=dp(15);bottomMargin=dp(8)});box.addView(textView("${formatBytes(used)} usados de ${formatBytes(total)}",12.5f,Color.rgb(147,192,163)));return box}
 private fun formatBytes(v:Double):String{val units=arrayOf("B","KB","MB","GB","TB");var n=v;var i=0;while(n>=1024&&i<units.lastIndex){n/=1024;i++};return if(i==0)"${n.toInt()} ${units[i]}" else String.format(java.util.Locale("pt","BR"),"%.1f %s",n,units[i])}
 private fun homeCard(title:String,sub:String,icon:String,action:()->Unit):View{val b=LinearLayout(this);b.gravity=Gravity.CENTER_VERTICAL;b.setPadding(dp(15),dp(10),dp(15),dp(10));b.background=rounded(Color.rgb(5,19,12),Color.rgb(29,75,49),18f);val ic=textView(icon,24f,Color.rgb(102,245,150));ic.gravity=Gravity.CENTER;b.addView(ic,LinearLayout.LayoutParams(dp(48),dp(48)).apply{rightMargin=dp(12)});val tx=LinearLayout(this);tx.orientation=LinearLayout.VERTICAL;val a=textView(title,15f,Color.WHITE);a.typeface=Typeface.DEFAULT_BOLD;tx.addView(a);tx.addView(textView(sub,12f,Color.rgb(135,178,150)));b.addView(tx,LinearLayout.LayoutParams(0,-2,1f));b.setOnClickListener{action()};return b}
 private fun bottomNav(selected:Int):View{
 val nav=LinearLayout(this);nav.gravity=Gravity.CENTER_VERTICAL;nav.setPadding(dp(6),dp(0),dp(6),dp(6));nav.setBackgroundColor(if(isDarkTheme)Color.rgb(16,25,20) else Color.WHITE);nav.elevation=dp(8).toFloat()
 val divider=View(this);divider.setBackgroundColor(if(isDarkTheme)Color.rgb(49,67,57) else Color.rgb(226,232,228));val wrapper=LinearLayout(this);wrapper.orientation=LinearLayout.VERTICAL;wrapper.addView(divider,LinearLayout.LayoutParams(-1,dp(1)));wrapper.addView(nav,LinearLayout.LayoutParams(-1,dp(79)));wrapper.clipChildren=false;wrapper.clipToPadding=false
 fun item(label:String,res:Int,index:Int,action:()->Unit):View{
  val box=LinearLayout(this);box.orientation=LinearLayout.VERTICAL;box.gravity=Gravity.CENTER;box.isClickable=true;box.isFocusable=true;box.setOnClickListener{action()}
  val frame=FrameLayout(this);frame.layoutParams=LinearLayout.LayoutParams(dp(56),dp(30))
  val pill=View(this);pill.background=rounded(if(isDarkTheme)Color.rgb(25,56,42) else Color.rgb(229,244,236),Color.TRANSPARENT,15f);pill.visibility=if(selected==index)View.VISIBLE else View.INVISIBLE;frame.addView(pill,FrameLayout.LayoutParams(-1,-1))
  val icon=ImageView(this);icon.setImageResource(res);icon.setColorFilter(if(selected==index)Color.rgb(19,122,84) else if(isDarkTheme)Color.rgb(166,190,175) else Color.rgb(112,128,118));icon.scaleType=ImageView.ScaleType.CENTER_INSIDE;frame.addView(icon,FrameLayout.LayoutParams(dp(24),dp(24),Gravity.CENTER))
  box.addView(frame)
  val t=textView(label,11f,if(selected==index)Color.rgb(19,122,84) else if(isDarkTheme)Color.rgb(166,190,175) else Color.rgb(112,128,118));t.gravity=Gravity.CENTER;t.typeface=Typeface.DEFAULT_BOLD;box.addView(t,LinearLayout.LayoutParams(-2,-2).apply{topMargin=dp(3)})
  return box
 }
 nav.addView(item("Início",R.drawable.ic_home,0){home()},LinearLayout.LayoutParams(0,-1,1f))
 nav.addView(item("Arquivos",R.drawable.ic_folder,1){filesPage()},LinearLayout.LayoutParams(0,-1,1f))
 val plusSlot=FrameLayout(this).apply{clipChildren=false}
 val plus=ImageButton(this).apply{setImageResource(R.drawable.ic_plus);setColorFilter(Color.WHITE);background=getDrawable(R.drawable.bg_fab);contentDescription="Criar documento";elevation=dp(6).toFloat();setPadding(dp(20),dp(20),dp(20),dp(20));translationY=-dp(22).toFloat();setOnClickListener{newDocument()}}
 plusSlot.addView(plus,FrameLayout.LayoutParams(dp(68),dp(68),Gravity.CENTER));nav.addView(plusSlot,LinearLayout.LayoutParams(0,-1,1f))
 nav.addView(item("Modelos",R.drawable.ic_templates,2){templatesPage()},LinearLayout.LayoutParams(0,-1,1f))
 nav.addView(item("Mais",R.drawable.ic_menu,3){morePage()},LinearLayout.LayoutParams(0,-1,1f))
 wrapper.addView(View(this),LinearLayout.LayoutParams(-1,dp(0)))
 return wrapper
}
private fun iconRes(icon:String):Int=when(icon){"▤"->R.drawable.ic_file;"★"->R.drawable.ic_star;"▰"->R.drawable.ic_folder;"◷"->R.drawable.ic_history;"⌫"->R.drawable.ic_trash;"＋"->R.drawable.ic_plus;"●"->R.drawable.ic_profile;"◇"->R.drawable.ic_info;"⚙"->R.drawable.ic_settings;"ⓘ"->R.drawable.ic_info;"↻"->R.drawable.ic_update;"♡"->R.drawable.ic_feedback;"✎"->R.drawable.ic_profile;else->R.drawable.ic_file}
 private fun newDocument(){book=Book(sheets=mutableListOf(Sheet(name="Planilha 1")));editor()}
 private fun storagePage(){simplePage("Armazenamento","Visão geral do espaço usado no seu celular"){val stat=android.os.StatFs(android.os.Environment.getDataDirectory().path);val total=stat.totalBytes.toDouble();val free=stat.availableBytes.toDouble();val used=(total-free).coerceAtLeast(0.0);val pct=((used/total)*100).coerceIn(0.0,100.0);val card=LinearLayout(this);card.orientation=LinearLayout.VERTICAL;card.setPadding(dp(20),dp(20),dp(20),dp(20));card.background=rounded(if(isDarkTheme)Color.rgb(23,44,32)else Color.WHITE,if(isDarkTheme)Color.rgb(52,91,68)else Color.rgb(208,228,216),22f);card.addView(textView(String.format(Locale("pt","BR"),"%.1f%% utilizado",pct),30f,inkColor()));card.addView(textView(formatBytes(used)+" de "+formatBytes(total),14f,mutedColor()),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(5);bottomMargin=dp(16)});val bar=ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);bar.max=1000;bar.progress=(pct*10).toInt();bar.progressDrawable=android.graphics.drawable.ClipDrawable(rounded(Color.rgb(101,255,157),Color.TRANSPARENT,10f),Gravity.LEFT,1);card.addView(bar,LinearLayout.LayoutParams(-1,dp(10)));it.addView(card,LinearLayout.LayoutParams(-1,dp(160)).apply{bottomMargin=dp(16)});it.addView(infoCard("Espaço do aparelho","O Nexa usa o armazenamento local do Android para arquivos baixados e documentos disponíveis offline."));it.addView(actionCard("Adicionar arquivo","Importe uma planilha ou documento do aparelho","＋"){addFilePage()});it.addView(actionCard("Documentos recentes","Veja os arquivos acessados por último","◷"){recentPage()})}}
 private fun filesPage(){simplePage("Arquivos","Tudo o que você cria, importa e organiza no Nexa"){it.addView(actionCard("Todos os documentos","Visualize todas as suas planilhas","▤"){allDocumentsPage()});it.addView(actionCard("Documentos favoritos","Acesse rapidamente seus favoritos","★"){favoritesPage()});it.addView(actionCard("Pastas","Organize seus documentos por pastas","▰"){foldersPage()});it.addView(actionCard("Atividade recente","Acompanhe as últimas ações","◷"){recentPage()});it.addView(actionCard("Lixeira","Documentos removidos ficam aqui","⌫"){trashPage()});it.addView(actionCard("Adicionar arquivo","Importar do dispositivo","＋"){addFilePage()})}}
 private fun allDocumentsPage(){simplePage("Todos os documentos","Suas planilhas e documentos"){it.addView(documentRow(book?.name?:"Nova planilha","Editado agora","▤"){editor()});it.addView(documentRow("Planilha de exemplo","Documento local","▤"){newDocument()});it.addView(actionCard("Criar documento","Comece uma nova planilha","＋"){newDocument()})}}
 private fun favoritesPage(){simplePage("Documentos favoritos","Seus documentos marcados como favoritos"){it.addView(infoCard("Ainda não há favoritos","Marque documentos como favoritos para encontrá-los aqui rapidamente."));it.addView(actionCard("Ver todos os documentos","Escolher um documento","▤"){allDocumentsPage()})}}
 private fun foldersPage(){simplePage("Pastas","Estruture seus documentos do seu jeito"){it.addView(actionCard("Criar pasta","Comece uma nova organização","＋"){showNexaToast("Nova pasta criada localmente",NexaToastType.SUCCESS)});it.addView(infoCard("Pastas vazias","Quando você criar uma pasta, ela aparecerá nesta área."))}}
 private fun recentPage(){simplePage("Atividade recente","Um resumo do que aconteceu no Nexa"){it.addView(activityRow("Nexa iniciado","Agora","Sistema"));it.addView(activityRow("Área de trabalho aberta","Agora","Nexa"));it.addView(activityRow("Última planilha acessada","Recentemente",book?.name?:"Nova planilha"));it.addView(infoCard("Sincronização","A sincronização pode ser iniciada pela área Mais quando houver uma sessão ativa."))}}
 private fun trashPage(){simplePage("Lixeira","Documentos removidos ficam separados dos arquivos ativos"){it.addView(infoCard("A lixeira está vazia","Nenhum documento foi enviado para a lixeira nesta sessão."));it.addView(actionCard("Esvaziar lixeira","Excluir permanentemente os itens da lixeira","⌫"){showNexaToast("A lixeira já está vazia",NexaToastType.INFO)})}}
 private fun addFilePage(){simplePage("Adicionar arquivo","Importe documentos do seu dispositivo"){it.addView(actionCard("Escolher arquivo","Abrir o seletor de arquivos do Android","＋"){val intent=Intent(Intent.ACTION_OPEN_DOCUMENT).apply{type="*/*";putExtra(Intent.EXTRA_ALLOW_MULTIPLE,false);addCategory(Intent.CATEGORY_OPENABLE)};startActivityForResult(intent,PICK)});it.addView(infoCard("Formatos","Escolha uma planilha ou documento compatível. O arquivo selecionado será preparado para uso no Nexa."))}}
 private fun profilePage(){simplePage("Meu perfil","Gerencie seus dados e preferências da conta"){
 it.addView(profileHero())
 val card=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(16),dp(16),dp(8));background=rounded(surfaceColor(),surfaceBorder(),18f)}
 val name=dialogInput("Nome exibido");name.setText(profileName)
 val email=dialogInput("E-mail");email.setText(getPreferences(0).getString("email","Conta Nexa")?:"Conta Nexa");email.isEnabled=false
 val city=dialogInput("Cidade");city.setText(getPreferences(0).getString("profileCity",""))
 val country=dialogInput("País");country.setText(getPreferences(0).getString("profileCountry",""))
 fun addField(label:String,e:EditText){card.addView(textView(label,12f,mutedColor()),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(4)});card.addView(e,LinearLayout.LayoutParams(-1,dp(52)).apply{topMargin=dp(4);bottomMargin=dp(8)})}
 addField("Nome",name);addField("E-mail",email);addField("Cidade",city);addField("País",country)
 val save=actionButton("Salvar alterações");card.addView(save,LinearLayout.LayoutParams(-1,dp(50)).apply{topMargin=dp(6);bottomMargin=dp(8)})
 save.setOnClickListener{profileName=name.text.toString().trim().ifBlank{"Meu perfil"};getPreferences(0).edit().putString("profileName",profileName).putString("profileCity",city.text.toString().trim()).putString("profileCountry",country.text.toString().trim()).apply();showNexaToast("Perfil atualizado",NexaToastType.SUCCESS);profilePage()}
 it.addView(card,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(16)})
 it.addView(actionCard("Meu plano","Consultar plano e recursos","◇"){planPage()})
 it.addView(actionCard("Configurações","Preferências do aplicativo","⚙"){settingsPage()})
 it.addView(actionCard("Sair","Encerrar esta sessão neste aparelho","⇥"){logout()})
}}
 private fun editProfile(){profilePage()}
 private fun planPage(){simplePage("Meu plano","Escolha e acompanhe o plano do Nexa"){
 val selected=getPreferences(0).getString("planId","free")?:"free"
 val plans=listOf(
  Triple("free","Free","Recursos essenciais para começar."),
  Triple("starter","Starter","Mais espaço e recursos para uso pessoal."),
  Triple("pro","Pro","Recursos avançados para produtividade."),
  Triple("business","Business","Colaboração e recursos para equipes."),
  Triple("enterprise","Enterprise","Recursos completos para organizações.")
 )
 it.addView(infoCard("Plano atual",plans.first{it.first==selected}.second+" • "+plans.first{it.first==selected}.third))
 for((id,name,desc) in plans){
  val card=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(14),dp(16),dp(14));background=rounded(if(id==selected)if(isDarkTheme)Color.rgb(23,55,36)else Color.rgb(239,249,243)else surfaceColor(),if(id==selected)if(isDarkTheme)Color.rgb(82,232,139)else Color.rgb(31,151,91)else surfaceBorder(),18f)}
  val row=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL}
  val title=textView(name,18f,inkColor());title.typeface=Typeface.DEFAULT_BOLD;row.addView(title,LinearLayout.LayoutParams(0,-2,1f))
  if(id==selected)row.addView(textView("ATUAL",10f,if(isDarkTheme)Color.rgb(82,232,139)else Color.rgb(19,122,84)))
  card.addView(row);card.addView(textView(desc,13f,mutedColor()),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(5)})
  val choose=actionButton(if(id==selected)"Plano atual" else "Selecionar plano");choose.isEnabled=id!=selected;card.addView(choose,LinearLayout.LayoutParams(-1,dp(44)).apply{topMargin=dp(11)});choose.setOnClickListener{getPreferences(0).edit().putString("planId",id).apply();showNexaToast("Plano $name selecionado",NexaToastType.SUCCESS);planPage()}
  it.addView(card,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(10)})
 }
}}
 private fun settingsPage(){simplePage("Configurações","Controle como o Nexa funciona neste aparelho"){
 fun boolSetting(title:String,sub:String,key:String,default:Boolean):View{
  val row=settingRow(title,sub,getPreferences(0).getBoolean(key,default))
  val sw=(row as ViewGroup).getChildAt(2) as Switch
  sw.setOnCheckedChangeListener{_,v->getPreferences(0).edit().putBoolean(key,v).apply();showNexaToast(if(v)"$title ativado" else "$title desativado",NexaToastType.SELECTION)}
  row.setOnClickListener{sw.isChecked=!sw.isChecked}
  return row
 }
 it.addView(infoCard("Preferências do aparelho","As opções são salvas localmente e controlam o comportamento do aplicativo."))
 it.addView(boolSetting("Tema escuro","Usar a aparência escura do Nexa","darkTheme",isDarkTheme).apply{
  val sw=(this as ViewGroup).getChildAt(2) as Switch
  sw.setOnCheckedChangeListener{_,checked->if(checked!=isDarkTheme){isDarkTheme=checked;getPreferences(0).edit().putBoolean("darkTheme",checked).apply();applySystemTheme();root.post{settingsPage()}}}
 })
 it.addView(boolSetting("Atualizações automáticas","Verificar novas versões ao iniciar","autoUpdates",true))
 it.addView(boolSetting("Confirmações","Confirmar ações importantes","confirmActions",true))
 it.addView(boolSetting("Notificações","Permitir avisos do aplicativo","notificationsEnabled",true))
 it.addView(boolSetting("Som de interface","Usar sons de confirmação","uiSounds",false))
 it.addView(boolSetting("Salvar automaticamente","Salvar alterações do editor periodicamente","autosave",true))
 it.addView(boolSetting("Sincronização automática","Sincronizar documentos quando houver conexão","autoSync",true))
 it.addView(boolSetting("Abrir último documento","Restaurar o último documento usado quando possível","openLastDocument",true))
 it.addView(actionCard("Idioma","Português (Brasil)","文"){nexaBuilder().setTitle("Idioma").setSingleChoiceItems(arrayOf("Português (Brasil)","English","Español"),getPreferences(0).getInt("languageIndex",0)){d,which->getPreferences(0).edit().putInt("languageIndex",which).apply();d.dismiss();showNexaToast("Idioma salvo",NexaToastType.SUCCESS)}.setNegativeButton("Cancelar",null).show()})
 it.addView(actionCard("Editor","Preferências de edição e planilha","▤"){editorSettingsPage()})
 it.addView(actionCard("Privacidade","Dados locais, sessão e permissões","●"){privacyPage()})
 it.addView(actionCard("Armazenamento","Uso de espaço e arquivos locais","▰"){storagePage()})
 it.addView(actionCard("Atualizações","Versão e atualizações disponíveis","↻"){updatesPage()})
 it.addView(actionCard("Dar feedback","Enviar uma mensagem para a equipe","♡"){feedbackPage()})
}}
 private fun editorSettingsPage(){simplePage("Editor","Preferências de edição e planilha"){
 fun boolSetting(title:String,sub:String,key:String,default:Boolean):View{
  val row=settingRow(title,sub,getPreferences(0).getBoolean(key,default));val sw=(row as ViewGroup).getChildAt(2) as Switch
  sw.setOnCheckedChangeListener{_,v->getPreferences(0).edit().putBoolean(key,v).apply();showNexaToast("Preferência salva",NexaToastType.SUCCESS)};row.setOnClickListener{sw.isChecked=!sw.isChecked};return row
 }
 it.addView(boolSetting("Abrir com a última aba","Restaurar a última aba usada","editorLastSheet",true))
 it.addView(boolSetting("Quebra automática de texto","Ajustar texto dentro das células","editorWrap",true))
 it.addView(boolSetting("Grade visível","Mostrar linhas da planilha","editorGrid",true))
 it.addView(boolSetting("Salvar ao sair","Salvar antes de fechar o editor","editorSaveOnExit",true))
 it.addView(infoCard("Zoom","O zoom é controlado pela categoria Exibir no editor."))
 it.addView(infoCard("Fórmulas","O editor suporta fórmulas, referências e operações de planilha na implementação atual."))
}}
private fun privacyPage(){simplePage("Privacidade","Controle de dados locais e sessão"){
 it.addView(infoCard("Dados locais","Preferências, sessão e informações de perfil deste aparelho são armazenadas localmente quando necessárias para o funcionamento do Nexa."))
 it.addView(actionCard("Sessão","Ver informações da sessão atual","●"){securityPage()})
 it.addView(actionCard("Permissões","Verificar permissões utilizadas","●"){permissionPage()})
 it.addView(actionCard("Restaurar preferências","Voltar às configurações padrão","⌫"){
  nexaBuilder().setTitle("Restaurar preferências?").setMessage("As preferências deste aparelho serão restauradas. Documentos no servidor não serão apagados.").setNegativeButton("Cancelar",null).setPositiveButton("Restaurar"){_,_->getPreferences(0).edit().clear().apply();isDarkTheme=false;applySystemTheme();showNexaToast("Preferências restauradas",NexaToastType.SUCCESS);settingsPage()}.show()
 })
}}
private fun permissionPage(){simplePage("Permissões","Estado das permissões necessárias ao Nexa"){
 it.addView(infoCard("Internet","Ativa • necessária para conta, sincronização, status e atualizações."))
 it.addView(infoCard("Instalação de atualizações","O Android controla esta permissão e pode solicitar autorização para instalar APKs oficiais."))
 it.addView(infoCard("Outras permissões","O Nexa não solicita câmera, contatos ou localização na versão atual."))
}}
private fun updatesPage(){simplePage("Atualizações","Versão instalada e disponibilidade"){
 val status=infoCard("Versão instalada","Nexa $APP_VERSION • código $APP_VERSION_CODE")
 it.addView(status)
 it.addView(actionCard("Procurar atualização","Consultar a versão oficial mais recente","↻"){checkForUpdate();showNexaToast("Verificando atualizações…",NexaToastType.SELECTION)})
 it.addView(infoCard("Distribuição","Atualizações oficiais são distribuídas pelo canal de releases do Nexa e instaladas somente após confirmação do usuário."))
}}
 private fun feedbackPage(){
 simplePage("Dar Feedback","Envie uma mensagem diretamente para a equipe do Nexa"){
  val intro=infoCard("Fale com a equipe","Seu feedback fica registrado no Nexa para análise da equipe. Escolha o tipo, dê um título e descreva o que aconteceu.")
  it.addView(intro)
  val form=LinearLayout(this).apply{
   orientation=LinearLayout.VERTICAL
   setPadding(dp(18),dp(18),dp(18),dp(18))
   background=rounded(surfaceColor(),surfaceBorder(),20f)
   elevation=dp(2).toFloat()
  }
  val typeLabel=textView("TIPO DE FEEDBACK",11f,mutedColor()).apply{typeface=Typeface.DEFAULT_BOLD;letterSpacing=.08f}
  form.addView(typeLabel)
  val category=Spinner(this).apply{
   background=rounded(if(isDarkTheme)Color.rgb(29,41,34)else Color.rgb(248,250,248),surfaceBorder(),14f)
   setPadding(dp(12),0,dp(12),0)
   adapter=ArrayAdapter<String>(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,listOf("Sugestão","Problema","Elogio","Solicitação","Outro"))
  }
  form.addView(category,LinearLayout.LayoutParams(-1,dp(50)).apply{topMargin=dp(7);bottomMargin=dp(15)})
  val subjectLabel=textView("TÍTULO",11f,mutedColor()).apply{typeface=Typeface.DEFAULT_BOLD;letterSpacing=.08f}
  form.addView(subjectLabel)
  val subject=inputField("Ex.: Sugestão para o editor").apply{
   background=rounded(surfaceColor(),surfaceBorder(),14f)
   setPadding(dp(15),0,dp(15),0)
  }
  form.addView(subject,LinearLayout.LayoutParams(-1,dp(52)).apply{topMargin=dp(7);bottomMargin=dp(15)})
  val messageLabel=textView("MENSAGEM",11f,mutedColor()).apply{typeface=Typeface.DEFAULT_BOLD;letterSpacing=.08f}
  form.addView(messageLabel)
  val message=EditText(this).apply{
   hint="Conte o que aconteceu ou o que você gostaria de ver no Nexa"
   setTextColor(inkColor())
   setHintTextColor(mutedColor())
   textSize=15f
   gravity=Gravity.TOP or Gravity.START
   minLines=7
   maxLines=10
   setPadding(dp(15),dp(14),dp(15),dp(14))
   background=rounded(surfaceColor(),surfaceBorder(),14f)
   setOnFocusChangeListener{_,focused->background=rounded(surfaceColor(),if(focused)Color.rgb(82,232,139)else surfaceBorder(),14f)}
  }
  form.addView(message,LinearLayout.LayoutParams(-1,dp(158)).apply{topMargin=dp(7);bottomMargin=dp(16)})
  val send=actionButton("Enviar feedback")
  form.addView(send,LinearLayout.LayoutParams(-1,dp(52)))
  send.setOnClickListener{sendFeedback(category.selectedItem.toString(),subject.text.toString().trim(),message.text.toString().trim(),send)}
  it.addView(form,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(16)})
 }
}
private fun securityPage(){simplePage("Segurança da conta","Controle sua sessão e proteção local"){it.addView(infoCard("Sessão","Sua sessão atual permanece protegida no armazenamento privado do aplicativo."));it.addView(infoCard("Atualizações","Instalações de produção são verificadas pelo fluxo oficial de atualização do Nexa."));it.addView(actionCard("Encerrar sessão","Remover a sessão deste aparelho e voltar ao login","⇥"){logout()});it.addView(actionCard("Limpar preferências locais","Remover preferências do aplicativo sem apagar dados do servidor","⌫"){nexaBuilder().setTitle("Limpar preferências?").setMessage("Isso removerá preferências locais como tema, plano selecionado e dados de perfil armazenados no aparelho.").setNegativeButton("Cancelar",null).setPositiveButton("Limpar"){_,_->getPreferences(0).edit().remove("darkTheme").remove("autoUpdates").remove("confirmActions").remove("planId").remove("profileCity").remove("profileCountry").apply();showNexaToast("Preferências locais limpas",NexaToastType.SUCCESS);profilePage()}.show()})}}
private fun aboutPage(){simplePage("Sobre o Nexa","Status do produto e informações públicas"){
 val hero=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;setPadding(dp(20),dp(18),dp(20),dp(18));background=rounded(if(isDarkTheme)Color.rgb(23,44,32)else Color.WHITE,if(isDarkTheme)Color.rgb(52,91,68)else Color.rgb(208,228,216),22f)}
 val logo=ImageView(this);logo.setImageResource(R.drawable.nexa_login_logo);logo.scaleType=ImageView.ScaleType.CENTER_INSIDE;hero.addView(logo,LinearLayout.LayoutParams(dp(72),dp(72)));val t=textView("Korczak Nexa",23f,inkColor());t.typeface=Typeface.DEFAULT_BOLD;hero.addView(t);hero.addView(textView("Seu espaço de produtividade.",13f,mutedColor()));it.addView(hero,LinearLayout.LayoutParams(-1,dp(160)).apply{bottomMargin=dp(14)})
 val status=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(12),dp(16),dp(12));background=rounded(surfaceColor(),surfaceBorder(),18f)}
 fun statusRow(label:String,value:String):TextView{val row=LinearLayout(this@MainActivity).apply{gravity=Gravity.CENTER_VERTICAL};val l=textView(label,13f,mutedColor());row.addView(l,LinearLayout.LayoutParams(0,dp(38),1f));val v=textView(value,13f,inkColor());v.typeface=Typeface.DEFAULT_BOLD;v.gravity=Gravity.CENTER_VERTICAL or Gravity.RIGHT;row.addView(v,LinearLayout.LayoutParams(dp(170),dp(38)));status.addView(row);return v}
 statusRow("API","NexaAPI");currentAboutApiVersionView=statusRow("Versão da API","Verificando…");statusRow("Versão do Nexa",APP_VERSION);currentAboutApiStatusView=statusRow("Aplicativo Android","Verificando…");statusRow("Aplicativo Desktop","Off-Line");statusRow("Versão Web/PWA","On-Line");statusRow("KZ HUB Integração","Off-Line");val updateStatus=statusRow("Atualizações","Verificando…");statusRow("Distribuição e Segurança","KZSecurity");statusRow("Sessão","Ativa")
 it.addView(status,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(14)})
 it.addView(actionCard("Verificar status","Atualizar disponibilidade da API e de novas versões","↻"){checkAboutStatus(updateStatus)})
 it.addView(infoCard("Produto","O Korczak Nexa é o aplicativo Android do Korczak HUB para criar, organizar, editar e sincronizar planilhas."))
 it.addView(infoCard("Compatibilidade","Aplicativo Android nativo. A versão desktop e a integração HUB permanecem independentes."))
 it.addView(infoCard("Segurança","As versões oficiais são distribuídas com assinatura de produção e o aplicativo solicita apenas permissões necessárias."))
 checkAboutStatus(updateStatus)
}}
 private fun morePage(){simplePage("Mais","Central de conta, aplicativo e suporte"){
 it.addView(actionCard("Meu perfil","Editar dados pessoais e preferências da conta","✎"){profilePage()})
 it.addView(actionCard("Meu plano","Planos, recursos e plano atual","◇"){planPage()})
 it.addView(actionCard("Configurações","Preferências, aparência e comportamento","⚙"){settingsPage()})
 it.addView(actionCard("Segurança da conta","Sessão, autenticação e proteção local","●"){securityPage()})
 it.addView(actionCard("Sobre o Nexa","Status público, versões e compatibilidade","ⓘ"){aboutPage()})
 it.addView(actionCard("Atualizações","Versão instalada e disponibilidade de atualização","↻"){updatesPage()})
 it.addView(actionCard("Dar Feedback","Enviar sugestões, problemas e elogios","♡"){feedbackPage()})
 it.addView(actionCard("Armazenamento","Espaço local e arquivos do aplicativo","▰"){storagePage()})
 it.addView(actionCard("Sincronização","Sincronizar a planilha atual com o NexaAPI","↻"){val id=book?.id;if(id.isNullOrBlank())showNexaToast("Salve a planilha antes de sincronizar.",NexaToastType.WARNING)else phase2Sync(id)})
 it.addView(actionCard("Sair","Encerrar a sessão neste aparelho","⇥"){logout()})
}}
 private fun simplePage(title:String,subtitle:String,build:(LinearLayout)->Unit){
 val target=PageTarget{renderSimplePage(title,subtitle,build)}
 if(!restoringPage){pageHistory.addLast(currentPageTarget?:PageTarget(null));if(pageHistory.size>30)pageHistory.removeFirst()}
 currentPageTarget=target
 renderSimplePage(title,subtitle,build)
}
private fun renderSimplePage(title:String,subtitle:String,build:(LinearLayout)->Unit){
 root.removeAllViews();applySystemTheme()
 val page=LinearLayout(this);page.orientation=LinearLayout.VERTICAL;page.setBackgroundColor(pageBg())
 val header=LinearLayout(this);header.gravity=Gravity.CENTER_VERTICAL;header.setPadding(dp(12),dp(8),dp(18),dp(8));header.setBackgroundColor(surfaceColor())
 val back=ImageView(this);back.setImageResource(R.drawable.ic_chevron);back.setPadding(dp(10),dp(10),dp(10),dp(10));back.setColorFilter(if(isDarkTheme)Color.rgb(82,232,139) else Color.rgb(19,122,84));back.contentDescription="Voltar";back.setOnClickListener{pageBack()};header.addView(back,LinearLayout.LayoutParams(dp(46),dp(48)))
 val texts=LinearLayout(this);texts.orientation=LinearLayout.VERTICAL;val h=textView(title,21f,inkColor());h.typeface=Typeface.DEFAULT_BOLD;texts.addView(h);texts.addView(textView(subtitle,11.5f,mutedColor()),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(2)});header.addView(texts,LinearLayout.LayoutParams(0,-2,1f))
 val av=textView(profileName.trim().split(Regex("\\s+")).filter{it.isNotEmpty()}.take(2).joinToString(""){it.first().uppercase()},11f,Color.WHITE);av.gravity=Gravity.CENTER;av.background=rounded(Color.rgb(19,122,84),Color.rgb(19,122,84),50f);av.setOnClickListener{profilePage()};header.addView(av,LinearLayout.LayoutParams(dp(38),dp(38)))
 page.addView(header,LinearLayout.LayoutParams(-1,dp(68)));val scroll=ScrollView(this);scroll.isFillViewport=true;scroll.setBackgroundColor(pageBg());val content=LinearLayout(this);content.orientation=LinearLayout.VERTICAL;content.setPadding(dp(18),dp(18),dp(18),dp(18));build(content);scroll.addView(content);page.addView(scroll,LinearLayout.LayoutParams(-1,0,1f));page.addView(bottomNav(navSelection(title)));root.addView(page)
}
 private fun navSelection(title:String):Int=when(title){"Arquivos","Todos os documentos","Documentos favoritos","Pastas","Atividade recente","Lixeira","Adicionar arquivo","Armazenamento"->1;"Modelos"->2;"Mais","Meu perfil","Meu plano","Configurações","Sobre o Nexa","Atualizações","Dar Feedback","Sair"->3;else->0}
 private fun templatesPage(){simplePage("Modelos","Modelos prontos para criar uma nova planilha"){
 it.addView(infoCard("Biblioteca de modelos","Escolha uma estrutura pronta. O modelo abre diretamente no editor para você personalizar e salvar."))
 val models=listOf(
  Triple("Orçamento","Controle receitas, despesas e saldo.","▤"),
  Triple("Planejamento","Organize tarefas, prazos e responsáveis.","◷"),
  Triple("Relatório","Estruture indicadores e resultados.","▤"),
  Triple("Controle financeiro","Acompanhe entradas, saídas e categorias.","▤"),
  Triple("Cronograma","Monte uma visão de atividades por período.","◷")
 )
 for((index,m) in models.withIndex()){
  val card=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(14),dp(14),dp(14),dp(14));background=rounded(surfaceColor(),surfaceBorder(),18f);elevation=dp(1).toFloat()}
  val preview=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;background=rounded(if(isDarkTheme)Color.rgb(16,28,21)else Color.rgb(246,249,247),surfaceBorder(),12f);setPadding(dp(7),dp(7),dp(7),dp(7))}
  for(rowIndex in 0 until 4){
   val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
   for(colIndex in 0 until 6){
    val cell=View(this);val highlighted=(rowIndex==0&&colIndex==(index%4)+1)||(rowIndex==2&&colIndex==3)
    cell.background=rounded(if(highlighted)if(isDarkTheme)Color.rgb(55,126,82)else Color.rgb(195,232,207)else if((rowIndex+colIndex)%3==0)if(isDarkTheme)Color.rgb(35,54,43)else Color.rgb(225,235,228)else if(isDarkTheme)Color.rgb(25,39,31)else Color.WHITE,surfaceBorder(),2f)
    row.addView(cell,LinearLayout.LayoutParams(0,dp(14),1f).apply{setMargins(dp(2),dp(2),dp(2),dp(2))})
   }
   preview.addView(row,LinearLayout.LayoutParams(-1,dp(18)))
  }
  card.addView(preview,LinearLayout.LayoutParams(-1,dp(82)))
  val title=textView(m.first,16f,inkColor());title.typeface=Typeface.DEFAULT_BOLD;card.addView(title,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(10)})
  card.addView(textView(m.second,12.5f,mutedColor()),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(3)})
  val use=actionButton("Usar modelo");card.addView(use,LinearLayout.LayoutParams(-1,dp(46)).apply{topMargin=dp(12)});use.setOnClickListener{newDocument()}
  it.addView(card,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(12)})
 }
}}
 private fun actionCard(title:String,sub:String,icon:String,action:()->Unit):View{
 val b=LinearLayout(this);b.gravity=Gravity.CENTER_VERTICAL;b.setPadding(dp(14),dp(12),dp(12),dp(12));b.background=rounded(surfaceColor(),surfaceBorder(),17f);b.elevation=dp(1).toFloat()
 val ic=ImageView(this);ic.setImageResource(iconRes(icon));ic.setColorFilter(Color.rgb(19,122,84));ic.scaleType=ImageView.ScaleType.CENTER_INSIDE;b.addView(ic,LinearLayout.LayoutParams(dp(44),dp(44)).apply{rightMargin=dp(12)})
 val tx=LinearLayout(this);tx.orientation=LinearLayout.VERTICAL;val a=textView(title,15f,inkColor());a.typeface=Typeface.DEFAULT_BOLD;tx.addView(a);tx.addView(textView(sub,12f,mutedColor()),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(3)});b.addView(tx,LinearLayout.LayoutParams(0,-2,1f))
 val arrow=ImageView(this);arrow.setImageResource(R.drawable.ic_chevron);arrow.setColorFilter(Color.rgb(19,122,84));arrow.rotation=180f;b.addView(arrow,LinearLayout.LayoutParams(dp(28),dp(28)));b.setOnClickListener{b.animate().alpha(.82f).setDuration(70).withEndAction{b.alpha=1f;action()}.start()};return b.apply{layoutParams=LinearLayout.LayoutParams(-1,dp(70)).apply{bottomMargin=dp(10)}}
}
 private fun infoCard(title:String,body:String):View{
 val b=LinearLayout(this);b.orientation=LinearLayout.VERTICAL;b.setPadding(dp(16),dp(15),dp(16),dp(15));b.background=rounded(if(isDarkTheme)Color.rgb(23,44,32)else Color.rgb(239,247,242),if(isDarkTheme)Color.rgb(52,91,68)else Color.rgb(208,228,216),17f);val t=textView(title,14f,if(isDarkTheme)Color.rgb(220,240,228)else Color.rgb(24,55,39));t.typeface=Typeface.DEFAULT_BOLD;b.addView(t);b.addView(textView(body,12.5f,if(isDarkTheme)Color.rgb(169,196,179)else Color.rgb(91,113,101)),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(6)});return b.apply{layoutParams=LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(12)}}
}
 private fun documentRow(title:String,sub:String,icon:String,action:()->Unit):View{return actionCard(title,sub,icon,action)}
 private fun activityRow(title:String,time:String,source:String):View{return infoCard(title,time+" • "+source)}
 private fun profileHero():View{val b=LinearLayout(this);b.gravity=Gravity.CENTER_VERTICAL;b.setPadding(dp(18),dp(18),dp(18),dp(18));b.background=rounded(Color.rgb(5,29,17),Color.rgb(47,117,72),22f);val av=textView(profileName.trim().split(Regex("\\s+")).filter{it.isNotEmpty()}.take(2).joinToString(""){it.first().uppercase()},18f,Color.rgb(3,25,14));av.gravity=Gravity.CENTER;av.typeface=Typeface.DEFAULT_BOLD;av.background=rounded(Color.rgb(111,248,158),Color.rgb(174,255,201),50f);b.addView(av,LinearLayout.LayoutParams(dp(64),dp(64)).apply{rightMargin=dp(14)});val tx=LinearLayout(this);tx.orientation=LinearLayout.VERTICAL;val n=textView(profileName,19f,Color.WHITE);n.typeface=Typeface.DEFAULT_BOLD;tx.addView(n);tx.addView(textView("Conta conectada ao Nexa",12f,Color.rgb(148,201,167)));b.addView(tx);return b.apply{layoutParams=LinearLayout.LayoutParams(-1,dp(102)).apply{bottomMargin=dp(16)}}}
 private fun settingRow(title:String,sub:String,enabled:Boolean):View{
 val row=LinearLayout(this);row.gravity=Gravity.CENTER_VERTICAL;row.setPadding(dp(14),dp(11),dp(10),dp(11));row.background=rounded(surfaceColor(),surfaceBorder(),17f);row.elevation=dp(1).toFloat();val ic=ImageView(this);ic.setImageResource(R.drawable.ic_settings);ic.setColorFilter(Color.rgb(19,122,84));row.addView(ic,LinearLayout.LayoutParams(dp(34),dp(34)).apply{rightMargin=dp(10)});val tx=LinearLayout(this);tx.orientation=LinearLayout.VERTICAL;val t=textView(title,14f,inkColor());t.typeface=Typeface.DEFAULT_BOLD;tx.addView(t);tx.addView(textView(sub,11.5f,Color.rgb(100,119,109)),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(2)});row.addView(tx,LinearLayout.LayoutParams(0,-2,1f));val sw=Switch(this);sw.id=View.generateViewId();sw.isChecked=enabled;row.addView(sw);return row.apply{layoutParams=LinearLayout.LayoutParams(-1,dp(70)).apply{bottomMargin=dp(10)}}
}
 private fun logout(){nexaBuilder().setTitle("Sair da conta?").setMessage("Sua sessão será encerrada neste aparelho. Você poderá entrar novamente depois.").setNegativeButton("Cancelar",null).setPositiveButton("Sair"){_,_->pageHistory.clear();getPreferences(0).edit().clear().apply();token=null;uid="";profileName="Meu perfil";login()}.show()}
 private fun editor(){
  currentPageTarget=PageTarget{editor()}
  root.removeAllViews()
  val page=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(pageBg())}
  val top=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(10),dp(8),dp(10),dp(8));setBackgroundColor(surfaceColor())}
  fun topIcon(label:String,click:()->Unit)=TextView(this).apply{text=label;gravity=Gravity.CENTER;textSize=19f;typeface=Typeface.DEFAULT_BOLD;setTextColor(inkColor());background=rounded(surfaceColor(),Color.TRANSPARENT,12f);setOnClickListener{click()};top.addView(this,LinearLayout.LayoutParams(dp(42),dp(42)))}
  topIcon("‹"){pageBack()}
  val titleBox=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(6),0,dp(4),0)}
  val title=textView(book?.name?.ifBlank{"Nova planilha"}?:"Nova planilha",16f,inkColor()).apply{typeface=Typeface.DEFAULT_BOLD;maxLines=1;ellipsize=android.text.TextUtils.TruncateAt.END}
  val meta=textView("Nexa • Planilha",11f,mutedColor())
  titleBox.addView(title);titleBox.addView(meta)
  top.addView(titleBox,LinearLayout.LayoutParams(0,dp(42),1f))
  topIcon("↶"){undo()};topIcon("↷"){redo()}
  val save=TextView(this).apply{text="Salvar";gravity=Gravity.CENTER;textSize=13f;typeface=Typeface.DEFAULT_BOLD;setTextColor(if(isDarkTheme)Color.rgb(4,24,13) else Color.WHITE);background=rounded(if(isDarkTheme)Color.rgb(82,232,139) else Color.rgb(19,122,84),Color.TRANSPARENT,13f);setOnClickListener{save()}}
  top.addView(save,LinearLayout.LayoutParams(dp(68),dp(40)).apply{leftMargin=dp(4)})
  page.addView(top)

  val formula=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(10),dp(7),dp(10),dp(7));setBackgroundColor(if(isDarkTheme)Color.rgb(15,23,19) else Color.rgb(242,246,243))}
  formula.addView(textView("fx",14f,if(isDarkTheme)Color.rgb(82,232,139) else Color.rgb(19,122,84)).apply{typeface=Typeface.DEFAULT_BOLD;gravity=Gravity.CENTER},LinearLayout.LayoutParams(dp(32),dp(36)))
  val formulaValue=EditText(this).apply{hint="Conteúdo da célula";setTextColor(inkColor());setHintTextColor(mutedColor());textSize=13f;setSingleLine(true);gravity=Gravity.CENTER_VERTICAL;setPadding(dp(10),0,dp(10),0);background=rounded(surfaceColor(),surfaceBorder(),10f);imeOptions=android.view.inputmethod.EditorInfo.IME_ACTION_DONE;setOnEditorActionListener{_,_,_->commitFormulaEditor();clearFocus();true};setOnFocusChangeListener{_,focused->if(!focused)commitFormulaEditor()}}
  formulaEditor=formulaValue
  formula.addView(formulaValue,LinearLayout.LayoutParams(0,dp(36),1f))
  formulaValue.addTextChangedListener(object:android.text.TextWatcher{override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int){};override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int){if(!formulaWriting&&formulaValue.hasFocus())updateCellFromFormula(s?.toString()?:"")};override fun afterTextChanged(s:android.text.Editable?){} })
  formula.addView(textView("⋮",22f,mutedColor()).apply{gravity=Gravity.CENTER;setOnClickListener{editorMoreMenu()}},LinearLayout.LayoutParams(dp(38),dp(36)))
  page.addView(formula)

  val categoryScroll=HorizontalScrollView(this).apply{isHorizontalScrollBarEnabled=false;setBackgroundColor(surfaceColor())}
  val categories=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setPadding(dp(8),dp(6),dp(8),dp(6))}
  categoryScroll.addView(categories,FrameLayout.LayoutParams(-2,-2));page.addView(categoryScroll)
  val subScroll=HorizontalScrollView(this).apply{isHorizontalScrollBarEnabled=false;setBackgroundColor(if(isDarkTheme)Color.rgb(18,27,22) else Color.rgb(247,249,247))}
  val subs=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setPadding(dp(8),dp(5),dp(8),dp(5))}
  subScroll.addView(subs,FrameLayout.LayoutParams(-2,-2));page.addView(subScroll)

  val gridHost=FrameLayout(this).apply{setBackgroundColor(surfaceColor())}
  grid=Grid();grid.isFocusableInTouchMode=true;grid.setOnKeyListener{_,keyCode,event->if(event.action!=KeyEvent.ACTION_DOWN)return@setOnKeyListener false;val ctrl=event.isCtrlPressed||event.isMetaPressed;val shift=event.isShiftPressed;when{ctrl&&keyCode==KeyEvent.KEYCODE_C->{copy();true};ctrl&&keyCode==KeyEvent.KEYCODE_X->{cut();true};ctrl&&keyCode==KeyEvent.KEYCODE_V->{paste();true};ctrl&&keyCode==KeyEvent.KEYCODE_Z->{undo();true};ctrl&&keyCode==KeyEvent.KEYCODE_Y->{redo();true};ctrl&&keyCode==KeyEvent.KEYCODE_A->{grid.selStart=0;grid.selEnd=199;grid.selColStart=0;grid.selColEnd=49;grid.invalidate();true};keyCode==KeyEvent.KEYCODE_DPAD_LEFT->{grid.selColStart=(grid.selColStart-1).coerceAtLeast(0);if(!shift)grid.selColEnd=grid.selColStart;grid.invalidate();syncFormulaEditor();true};keyCode==KeyEvent.KEYCODE_DPAD_RIGHT->{grid.selColEnd=(grid.selColEnd+1).coerceAtMost(49);if(!shift)grid.selColStart=grid.selColEnd;grid.invalidate();syncFormulaEditor();true};keyCode==KeyEvent.KEYCODE_DPAD_UP->{grid.selStart=(grid.selStart-1).coerceAtLeast(0);if(!shift)grid.selEnd=grid.selStart;grid.invalidate();syncFormulaEditor();true};keyCode==KeyEvent.KEYCODE_DPAD_DOWN->{grid.selEnd=(grid.selEnd+1).coerceAtMost(199);if(!shift)grid.selStart=grid.selEnd;grid.invalidate();syncFormulaEditor();true};keyCode==KeyEvent.KEYCODE_TAB->{grid.selColStart=(grid.selColEnd+1).coerceAtMost(49);grid.selColEnd=grid.selColStart;syncFormulaEditor();grid.invalidate();true};keyCode==KeyEvent.KEYCODE_ENTER->{grid.selStart=(grid.selEnd+1).coerceAtMost(199);grid.selEnd=grid.selStart;syncFormulaEditor();grid.invalidate();true};else->false}};gridHost.addView(grid,FrameLayout.LayoutParams(-1,-1));page.addView(gridHost,LinearLayout.LayoutParams(-1,0,1f))

  val sheetBar=HorizontalScrollView(this).apply{isHorizontalScrollBarEnabled=false;setBackgroundColor(if(isDarkTheme)Color.rgb(16,25,20) else Color.WHITE)}
  val sheets=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(8),dp(6),dp(8),dp(6))}
  fun refreshSheets(){sheets.removeAllViews();book?.sheets?.forEachIndexed{idx,s->val chip=TextView(this).apply{text=s.name;gravity=Gravity.CENTER;textSize=12f;typeface=if(idx==book?.active)Typeface.DEFAULT_BOLD else Typeface.DEFAULT;setTextColor(if(idx==book?.active){if(isDarkTheme)Color.rgb(82,232,139)else Color.rgb(19,122,84)}else mutedColor());background=rounded(if(idx==book?.active){if(isDarkTheme)Color.rgb(25,56,42)else Color.rgb(235,246,239)}else Color.TRANSPARENT,Color.TRANSPARENT,12f);setPadding(dp(14),0,dp(14),0);setOnClickListener{book?.active=idx;grid.invalidate();refreshSheets()}};sheets.addView(chip,LinearLayout.LayoutParams(-2,dp(38)).apply{rightMargin=dp(5)})};val add=TextView(this).apply{text="+";gravity=Gravity.CENTER;textSize=21f;setTextColor(if(isDarkTheme)Color.rgb(82,232,139)else Color.rgb(19,122,84));background=rounded(surfaceColor(),surfaceBorder(),12f);setOnClickListener{addSheet();refreshSheets()}};sheets.addView(add,LinearLayout.LayoutParams(dp(42),dp(38)))}
  refreshSheets();sheetBar.addView(sheets,FrameLayout.LayoutParams(-2,-2));page.addView(sheetBar)

  fun action(label:String,sub:String,click:()->Unit){val box=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(10),dp(6),dp(10),dp(6));background=rounded(surfaceColor(),Color.TRANSPARENT,12f);setOnClickListener{click()}};val ic=ImageView(this).apply{setImageResource(editorIcon(label));setColorFilter(if(isDarkTheme)Color.rgb(82,232,139)else Color.rgb(19,122,84));contentDescription=label};box.addView(ic,LinearLayout.LayoutParams(dp(22),dp(22)).apply{rightMargin=dp(7)});val tx=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};tx.addView(textView(label,12.5f,inkColor()).apply{typeface=Typeface.DEFAULT_BOLD;maxLines=1});tx.addView(textView(sub,9.5f,mutedColor()).apply{maxLines=1});box.addView(tx)
   subs.addView(box,LinearLayout.LayoutParams(-2,dp(52)).apply{rightMargin=dp(5)})}
  fun category(name:String){val chip=TextView(this).apply{text=name;gravity=Gravity.CENTER;textSize=13f;typeface=Typeface.DEFAULT_BOLD;setTextColor(inkColor());setPadding(dp(16),0,dp(16),0);background=rounded(surfaceColor(),Color.TRANSPARENT,13f)};categories.addView(chip,LinearLayout.LayoutParams(-2,dp(38)).apply{rightMargin=dp(5)});chip.setOnClickListener{for(i in 0 until categories.childCount){val v=categories.getChildAt(i);v.background=rounded(surfaceColor(),Color.TRANSPARENT,13f);v.findViewById<TextView>(android.R.id.text1)?.setTextColor(inkColor())};chip.background=rounded(if(isDarkTheme)Color.rgb(25,56,42)else Color.rgb(235,246,239),Color.TRANSPARENT,13f);subs.removeAllViews();when(name){
    "Início"->{action("Negrito","B"){toggle("b")};action("Itálico","I"){toggle("i")};action("Sublinhado","U"){toggle("u")};action("Tachado","S"){toggle("s")};action("Alinhar","Horizontal"){alignmentMenu()};action("Tamanho","Fonte"){fontSize()};action("Cor texto","Fonte"){fontColorMenu()};action("Cor fundo","Célula"){backgroundColorMenu()};action("Quebra","Texto"){wrap()};action("Bordas","Células"){border()};action("Número","Formato"){numberFormatMenu()};action("Preencher","Células"){autoFill()}}
    "Inserir"->{action("Nova aba","Planilha"){addSheet();refreshSheets()};action("Inserir linha","Abaixo"){insertRows()};action("Inserir coluna","À direita"){insertCols()};action("Excluir linha","Selecionada"){deleteRows()};action("Excluir coluna","Selecionada"){deleteCols()};action("Documento","Novo"){newDocument()};action("Importar","Arquivo"){importFile()};action("Imagem","Arquivo"){importFile()};action("Tabela","Dados"){createLocalTable()};action("Comentário","Célula"){editComment()}}
    "Formatar"->{action("Estilo","Célula"){cellStyle()};action("Condicional","Regras"){conditional()};action("Validação","Dados"){validation()};action("Número","Formato"){numberFormatMenu()};action("Bordas","Contorno"){border()};action("Preencher","Conteúdo"){autoFill()}}
    "Dados"->{action("Ordenar","Intervalo"){sortSelection()};action("Filtrar","Dados"){filterSelection()};action("Agrupar linha","Linhas"){groupRow()};action("Agrupar coluna","Colunas"){groupCol()};action("Grupos +/-","Expandir"){toggleGroups()};action("Mesclar","Células"){merge()};action("Desmesclar","Células"){unmerge()}}
    "Exibir"->{action("Zoom +","Ampliar"){grid.zoom*=1.15f;grid.invalidate()};action("Largura","Coluna"){resizeColumn()};action("Altura","Linha"){resizeRow()};action("Zoom -","Reduzir"){grid.zoom=maxOf(.55f,grid.zoom/1.15f);grid.invalidate()};action("Congelar","Painéis"){freeze()};action("Ocultar","Linhas/colunas"){hide()};action("Mostrar","Linhas/colunas"){show()}}
    "Arquivo"->{action("Salvar","Nexa"){save()};action("Exportar","CSV/TSV/Nexa"){exportFile()};action("Copiar","Seleção"){copy()};action("Recortar","Seleção"){cut()};action("Colar","Área de transferência"){paste()};action("Atualizar","Sincronizar"){phase2()};action("Sair","Editor"){home()}}
  }}}
  listOf("Início","Inserir","Formatar","Dados","Exibir","Arquivo").forEach{category(it)}
  (categories.getChildAt(0) as View).performClick()
  root.addView(page)
 }
 private fun editorIcon(label:String):Int=when(label){
 "Negrito"->R.drawable.ic_editor_bold;"Itálico"->R.drawable.ic_editor_italic;"Sublinhado"->R.drawable.ic_editor_underline;"Tachado"->R.drawable.ic_editor_strike;"Alinhar"->R.drawable.ic_editor_align;"Tamanho"->R.drawable.ic_editor_size;"Cor texto"->R.drawable.ic_editor_fill_color;"Cor fundo"->R.drawable.ic_editor_fill_color;"Quebra"->R.drawable.ic_editor_wrap;"Bordas"->R.drawable.ic_editor_border_all;"Número"->R.drawable.ic_editor_number;"Preencher"->R.drawable.ic_editor_fill_color;
 "Nova aba"->R.drawable.ic_editor_add_sheet;"Documento"->R.drawable.ic_editor_document;"Importar"->R.drawable.ic_editor_import;"Imagem"->R.drawable.ic_editor_image;"Tabela"->R.drawable.ic_editor_table;"Comentário"->R.drawable.ic_editor_comment;
 "Estilo"->R.drawable.ic_editor_style;"Condicional"->R.drawable.ic_editor_conditional;"Validação"->R.drawable.ic_editor_validation;
 "Ordenar"->R.drawable.ic_editor_sort;"Filtrar"->R.drawable.ic_editor_filter;"Agrupar linha"->R.drawable.ic_editor_group_row;"Agrupar coluna"->R.drawable.ic_editor_group_col;"Grupos +/-"->R.drawable.ic_editor_groups;"Mesclar"->R.drawable.ic_editor_merge;"Desmesclar"->R.drawable.ic_editor_unmerge;
 "Zoom +"->R.drawable.ic_editor_zoom_in;"Zoom -"->R.drawable.ic_editor_zoom_out;"Congelar"->R.drawable.ic_editor_freeze;"Ocultar"->R.drawable.ic_editor_hide;"Mostrar"->R.drawable.ic_editor_show;
 "Salvar"->R.drawable.ic_editor_save;"Exportar"->R.drawable.ic_editor_export;"Copiar"->R.drawable.ic_editor_copy;"Colar"->R.drawable.ic_editor_paste;"Atualizar"->R.drawable.ic_editor_refresh;"Sair"->R.drawable.ic_editor_exit;else->R.drawable.ic_file}
 private fun createLocalTable(){snap();val s=book!!.sheets[book!!.active];for(r in grid.selStart..grid.selEnd)for(k in grid.selColStart..grid.selColEnd){val ce=s.cells[key(r,k)]?:Cell();ce.borderTop=true;ce.borderRight=true;ce.borderBottom=true;ce.borderLeft=true;if(r==grid.selStart){ce.bold=true;ce.background=if(isDarkTheme)Color.rgb(25,56,42)else Color.rgb(232,244,237)};s.cells[key(r,k)]=ce};grid.invalidate();showNexaToast("Tabela formatada no intervalo selecionado",NexaToastType.SUCCESS)}
private fun editComment(){val s=book!!.sheets[book!!.active];val k=key(grid.selStart,grid.selColStart);val input=dialogInput("Comentário");input.setText(s.cells[k]?.comment?:"");nexaBuilder().setTitle("Comentário • $k").setView(input).setPositiveButton("Salvar"){_,_->snap();val ce=s.cells[k]?:Cell();ce.comment=input.text.toString();s.cells[k]=ce;grid.invalidate();showNexaToast("Comentário salvo",NexaToastType.SUCCESS)}.setNegativeButton("Cancelar",null).show()}
private fun cellStyle(){val opts=arrayOf("Padrão","Título","Destaque","Total");nexaBuilder().setTitle("Estilo da célula").setItems(opts){_,which->snap();val s=book!!.sheets[book!!.active];for(r in grid.selStart..grid.selEnd)for(k in grid.selColStart..grid.selColEnd){val ce=s.cells[key(r,k)]?:Cell();when(which){0->{ce.bold=false;ce.fontSize=14f};1->{ce.bold=true;ce.fontSize=18f};2->{ce.bold=true;ce.background=if(isDarkTheme)Color.rgb(25,56,42)else Color.rgb(232,244,237)};3->{ce.bold=true;ce.numberFormat="currency"}};ce.fontColor=if(isDarkTheme)Color.rgb(239,248,242)else Color.rgb(35,48,41);s.cells[key(r,k)]=ce};grid.invalidate()}.show()}
private fun parseCellRef(x:String):Pair<Int,Int>{val m=Regex("([A-Z]+)([0-9]+)",RegexOption.IGNORE_CASE).find(x.trim())?:throw IllegalArgumentException("Referência inválida");var n=0;for(ch in m.groupValues[1].uppercase())n=n*26+ch.code-64;return Pair(m.groupValues[2].toInt()-1,n-1)}
private fun insertRows(){snap();val s=book!!.sheets[book!!.active];val at=grid.selStart;val count=grid.selEnd-at+1;val out=mutableMapOf<String,Cell>();for((k,v) in s.cells){val p=parseCellRef(k);if(p.first>=at)out[key(p.first+count,p.second)]=v.copy()else out[k]=v.copy()};s.cells.clear();s.cells.putAll(out);s.hiddenRows=s.hiddenRows.map{if(it>=at)it+count else it}.toMutableSet();s.groupedRows=s.groupedRows.map{if(it>=at)it+count else it}.toMutableSet();s.rowGroups=s.rowGroups.map{shiftIndexGroup(it,at,count)}.toMutableList();s.rowHeights=s.rowHeights.entries.associate{(k,v)->(if(k>=at)k+count else k) to v}.toMutableMap();collapsedRows.clear();s.merged=s.merged.map{shiftRangeRows(it,at,count)}.toMutableSet();shiftAllFormulaRefs(s,at,count,0);grid.selStart=at;grid.selEnd=at+count-1;grid.invalidate();showNexaToast("$count linha(s) inserida(s)",NexaToastType.SUCCESS)}
private fun insertCols(){snap();val s=book!!.sheets[book!!.active];val at=grid.selColStart;val count=grid.selColEnd-at+1;val out=mutableMapOf<String,Cell>();for((k,v) in s.cells){val p=parseCellRef(k);if(p.second>=at)out[key(p.first,p.second+count)]=v.copy()else out[k]=v.copy()};s.cells.clear();s.cells.putAll(out);s.hiddenCols=s.hiddenCols.map{if(it>=at)it+count else it}.toMutableSet();s.groupedCols=s.groupedCols.map{if(it>=at)it+count else it}.toMutableSet();s.colGroups=s.colGroups.map{shiftIndexGroup(it,at,count)}.toMutableList();s.columnWidths=s.columnWidths.entries.associate{(k,v)->(if(k>=at)k+count else k) to v}.toMutableMap();collapsedCols.clear();s.merged=s.merged.map{shiftRangeCols(it,at,count)}.toMutableSet();shiftAllFormulaRefs(s,0,0,count,true,at+1,false);grid.selColStart=at;grid.selColEnd=at+count-1;grid.invalidate();showNexaToast("$count coluna(s) inserida(s)",NexaToastType.SUCCESS)}
private fun deleteRows(){snap();val s=book!!.sheets[book!!.active];val at=grid.selStart;val count=grid.selEnd-at+1;val out=mutableMapOf<String,Cell>();for((k,v) in s.cells){val p=parseCellRef(k);if(p.first in at..grid.selEnd)continue;val nr=if(p.first>grid.selEnd)p.first-count else p.first;out[key(nr,p.second)]=v.copy()};s.cells.clear();s.cells.putAll(out);s.hiddenRows=s.hiddenRows.filter{it !in at..grid.selEnd}.map{if(it>grid.selEnd)it-count else it}.toMutableSet();s.groupedRows=s.groupedRows.filter{it !in at..grid.selEnd}.map{if(it>grid.selEnd)it-count else it}.toMutableSet();s.rowGroups=s.rowGroups.mapNotNull{deleteIndexGroup(it,at,count)}.toMutableList();s.rowHeights=s.rowHeights.entries.mapNotNull{(k,v)->when{ k in at..grid.selEnd->null; k>grid.selEnd->(k-count) to v; else->k to v}}.toMap().toMutableMap();collapsedRows.clear();s.merged=s.merged.mapNotNull{deleteRangeRows(it,at,count)}.toMutableSet();shiftAllFormulaRefs(s,at,count,0,true);grid.selStart=(at.coerceAtMost(199-count)).coerceAtLeast(0);grid.selEnd=grid.selStart;grid.invalidate();showNexaToast("$count linha(s) excluída(s)",NexaToastType.SUCCESS)}
private fun deleteCols(){snap();val s=book!!.sheets[book!!.active];val at=grid.selColStart;val count=grid.selColEnd-at+1;val out=mutableMapOf<String,Cell>();for((k,v) in s.cells){val p=parseCellRef(k);if(p.second in at..grid.selColEnd)continue;val nc=if(p.second>grid.selColEnd)p.second-count else p.second;out[key(p.first,nc)]=v.copy()};s.cells.clear();s.cells.putAll(out);s.hiddenCols=s.hiddenCols.filter{it !in at..grid.selColEnd}.map{if(it>grid.selColEnd)it-count else it}.toMutableSet();s.groupedCols=s.groupedCols.filter{it !in at..grid.selColEnd}.map{if(it>grid.selColEnd)it-count else it}.toMutableSet();s.colGroups=s.colGroups.mapNotNull{deleteIndexGroup(it,at,count)}.toMutableList();s.columnWidths=s.columnWidths.entries.mapNotNull{(k,v)->when{ k in at..grid.selColEnd->null; k>grid.selColEnd->(k-count) to v; else->k to v}}.toMap().toMutableMap();collapsedCols.clear();s.merged=s.merged.mapNotNull{deleteRangeCols(it,at,count)}.toMutableSet();shiftAllFormulaRefs(s,0,0,count,true,at+1,true);grid.selColStart=(at.coerceAtMost(49-count)).coerceAtLeast(0);grid.selColEnd=grid.selColStart;grid.invalidate();showNexaToast("$count coluna(s) excluída(s)",NexaToastType.SUCCESS)}
private fun shiftRangeRows(r:String,at:Int,d:Int):String{val p=r.split(":");fun q(x:String):String{val a=parseCellRef(x);return key(if(a.first>=at)a.first+d else a.first,a.second)};return if(p.size==2)q(p[0])+":"+q(p[1]) else q(p[0])}
private fun shiftRangeCols(r:String,at:Int,d:Int):String{val p=r.split(":");fun q(x:String):String{val a=parseCellRef(x);return key(a.first,if(a.second>=at)a.second+d else a.second)};return if(p.size==2)q(p[0])+":"+q(p[1]) else q(p[0])}
private fun deleteRangeRows(r:String,at:Int,d:Int):String?{val p=r.split(":");if(p.size!=2)return if(parseCellRef(p[0]).first in at until at+d)null else shiftRangeRows(r,at,-d);val a=parseCellRef(p[0]);val b=parseCellRef(p[1]);if(a.first>=at&&b.first<at+d)return null;return key(if(a.first>=at+d)a.first-d else a.first,a.second)+":"+key(if(b.first>=at+d)b.first-d else b.first,b.second)}
private fun deleteRangeCols(r:String,at:Int,d:Int):String?{val p=r.split(":");if(p.size!=2)return if(parseCellRef(p[0]).second in at until at+d)null else shiftRangeCols(r,at,-d);val a=parseCellRef(p[0]);val b=parseCellRef(p[1]);if(a.second>=at&&b.second<at+d)return null;return key(a.first,if(a.second>=at+d)a.second-d else a.second)+":"+key(b.first,if(b.second>=at+d)b.second-d else b.second)}
private fun shiftAllFormulaRefs(s:Sheet,rowAt:Int,rowDelta:Int,colDelta:Int,insert:Boolean=true,colAt:Int=0,delete:Boolean=false){val snapshot=s.cells.toMap();for((k,ce) in snapshot){if(!ce.input.startsWith("="))continue;var f=ce.input;f=f.replace(Regex("(\$?)([A-Z]+)(\$?)([0-9]+)")){m->var n=0;for(ch in m.groupValues[2])n=n*26+ch.code-64;var rr=m.groupValues[4].toInt();var cc=n;var refError=false;if(rowAt>0){if(delete&&rr in rowAt+1..rowAt+rowDelta)refError=true else if(rr>=rowAt+1)rr+=if(delete)-rowDelta else rowDelta};if(colAt>0){if(delete&&cc in colAt..colAt+colDelta-1)refError=true else if(cc>=colAt)cc+=if(delete)-colDelta else colDelta};if(refError||rr<1||cc<1)"#REF!" else (if(m.groupValues[1]=="$")"$" else "")+col(cc-1)+(if(m.groupValues[3]=="$")"$" else "")+rr};s.cells[k]=ce.copy(input=f)}}
private fun resizeColumn(){val s=book!!.sheets[book!!.active];val current=s.columnWidths[grid.selColStart]?:grid.cw;val input=dialogInput("Largura em dp");input.inputType=2;input.setText(current.toInt().toString());nexaBuilder().setTitle("Largura da(s) coluna(s)").setView(input).setPositiveButton("Aplicar"){_,_->val n=input.text.toString().toFloatOrNull()?.coerceIn(48f,500f)?:current;snap();for(k in grid.selColStart..grid.selColEnd)s.columnWidths[k]=n;grid.invalidate()}.setNegativeButton("Cancelar",null).show()}
private fun resizeRow(){val s=book!!.sheets[book!!.active];val current=s.rowHeights[grid.selStart]?:grid.rh;val input=dialogInput("Altura em dp");input.inputType=2;input.setText(current.toInt().toString());nexaBuilder().setTitle("Altura da(s) linha(s)").setView(input).setPositiveButton("Aplicar"){_,_->val n=input.text.toString().toFloatOrNull()?.coerceIn(28f,180f)?:current;snap();for(r in grid.selStart..grid.selEnd)s.rowHeights[r]=n;grid.invalidate()}.setNegativeButton("Cancelar",null).show()}
private fun sortSelection(){val s=book!!.sheets[book!!.active];if(grid.selStart>=grid.selEnd){showNexaToast("Selecione pelo menos 2 linhas",NexaToastType.WARNING);return};val rows=(grid.selStart..grid.selEnd).toList();val entries=rows.map{it to (s.cells[key(it,grid.selColStart)]?.input?:"")};val numeric=entries.all{it.second.replace(",",".").toDoubleOrNull()!=null||it.second.isBlank()};val ordered=if(numeric)entries.sortedWith(compareBy<Pair<Int,String>>{it.second.replace(",",".").toDoubleOrNull()?:Double.POSITIVE_INFINITY}.thenBy{it.first})else entries.sortedWith(compareBy<String>{it.second.lowercase()}.thenBy{it.first});snap();val blocks=ordered.map{(row,_)->(grid.selColStart..grid.selColEnd).map{k->s.cells[key(row,k)]?.copy()?:Cell()}};for((i,row) in rows.withIndex())for((j,k) in (grid.selColStart..grid.selColEnd).withIndex())s.cells[key(row,k)]=blocks[i][j];grid.invalidate();showNexaToast("Intervalo ordenado",NexaToastType.SUCCESS)}
private fun filterSelection(){val s=book!!.sheets[book!!.active];val input=dialogInput("Valor; vazio remove o filtro");nexaBuilder().setTitle("Filtrar coluna").setView(input).setPositiveButton("Aplicar"){_,_->val needle=input.text.toString();snap();for(r in grid.selStart..grid.selEnd){val value=s.cells[key(r,grid.selColStart)]?.input?:"";if(needle.isBlank()||value.contains(needle,true))s.hiddenRows.remove(r)else s.hiddenRows.add(r)};grid.invalidate();showNexaToast(if(needle.isBlank())"Filtro removido" else "Filtro aplicado",NexaToastType.SUCCESS)}.setNegativeButton("Cancelar",null).show()}
private fun editorMoreMenu(){nexaBuilder().setTitle("Editor").setItems(arrayOf("Salvar","Nexa Completo","Exportar","Importar","Atualizar dados","Fechar editor")){_,which->when(which){0->save();1->phase2();2->exportFile();3->importFile();4->phase2Sync(book?.id?:"");5->pageBack()}}.show()}
 private var formulaEditor:EditText?=null
 private var formulaWriting=false
 private fun syncFormulaEditor(){val e=formulaEditor?:return;val s=book?.sheets?.getOrNull(book?.active?:0)?:return;val v=s.cells[key(grid.selStart,grid.selColStart)]?.input?:"";if(e.text.toString()!=v){formulaWriting=true;e.setText(v);e.setSelection(e.length());formulaWriting=false}}
 private var formulaDraftSnap=false
 private fun updateCellFromFormula(v:String){val s=book?.sheets?.getOrNull(book?.active?:0)?:return;val k=key(grid.selStart,grid.selColStart);if(!valid(v,s.validations[k]))return;if(!formulaDraftSnap){snap();formulaDraftSnap=true};if(v.isBlank())s.cells.remove(k)else{s.cells[k]=s.cells[k]?.also{it.input=v}?:Cell(v)};grid.invalidate()}
 private fun commitFormulaEditor(){if(formulaWriting)return;val e=formulaEditor?:return;val s=book?.sheets?.getOrNull(book?.active?:0)?:return;val v=e.text.toString();val k=key(grid.selStart,grid.selColStart);if(v==(s.cells[k]?.input?:"")){formulaDraftSnap=false;return};if(!valid(v,s.validations[k])){showNexaToast("Valor inválido",NexaToastType.ERROR);syncFormulaEditor();formulaDraftSnap=false;return};if(!formulaDraftSnap)snap();formulaDraftSnap=false;if(v.isBlank())s.cells.remove(k)else{s.cells[k]=s.cells[k]?.also{it.input=v}?:Cell(v)};grid.invalidate()}
 private data class EditorHistory(val state:String,val row:Int,val rowEnd:Int,val col:Int,val colEnd:Int)
 private val history=ArrayDeque<EditorHistory>();private val future=ArrayDeque<EditorHistory>();private val collapsedRows=mutableSetOf<String>();private val collapsedCols=mutableSetOf<String>()
 private fun currentHistoryState():EditorHistory=EditorHistory(toJson(book!!).toString(),grid.selStart,grid.selEnd,grid.selColStart,grid.selColEnd)
 private fun restoreHistoryState(h:EditorHistory){book=from(JSONObject(h.state));grid.selStart=h.row.coerceAtLeast(0);grid.selEnd=h.rowEnd.coerceAtLeast(grid.selStart);grid.selColStart=h.col.coerceAtLeast(0);grid.selColEnd=h.colEnd.coerceAtLeast(grid.selColStart);syncFormulaEditor();grid.invalidate()}
 private fun snap(){val h=currentHistoryState();if(history.lastOrNull()?.state!=h.state)history.addLast(h);if(history.size>100)history.removeFirst();future.clear()}
 private fun undo(){if(history.isEmpty()){showNexaToast("Nada para desfazer",NexaToastType.INFO);return};val h=history.removeLast();future.addFirst(currentHistoryState());restoreHistoryState(h)}
 private fun redo(){if(future.isEmpty()){showNexaToast("Nada para refazer",NexaToastType.INFO);return};val h=future.removeFirst();history.addLast(currentHistoryState());restoreHistoryState(h)}
 private fun addSheet(){snap();book!!.sheets.add(Sheet(name="Planilha "+(book!!.sheets.size+1)));book!!.active=book!!.sheets.lastIndex;grid.invalidate()}
 private fun edit(r:Int,c:Int){
 grid.selStart=r;grid.selEnd=r;grid.selColStart=c;grid.selColEnd=c
 syncFormulaEditor()
 formulaEditor?.requestFocus()
 formulaEditor?.selectAll()
 (getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager).showSoftInput(formulaEditor,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
 grid.invalidate()
}
 private fun toggle(t:String){snap();val s=book!!.sheets[book!!.active];for(r in grid.selStart..grid.selEnd)for(k in grid.selColStart..grid.selColEnd){val ce=s.cells[key(r,k)]?:Cell();if(t=="b")ce.bold=!ce.bold;if(t=="i")ce.italic=!ce.italic;if(t=="u")ce.underline=!ce.underline;if(t=="s")ce.strike=!ce.strike;s.cells[key(r,k)]=ce};grid.invalidate()}
 private fun align(a:Int){snap();val s=book!!.sheets[book!!.active];for(r in grid.selStart..grid.selEnd)for(k in grid.selColStart..grid.selColEnd){val ce=s.cells[key(r,k)]?:Cell();ce.align=a;s.cells[key(r,k)]=ce};grid.invalidate()}
private fun alignmentMenu(){nexaBuilder().setTitle("Alinhamento").setItems(arrayOf("Esquerda","Centro","Direita")){_,which->align(which)}.show()}
private fun colorOptions()=intArrayOf(Color.rgb(35,48,41),Color.rgb(19,122,84),Color.rgb(36,99,235),Color.rgb(190,45,45),Color.rgb(150,75,0),Color.WHITE,Color.rgb(255,245,170),Color.rgb(220,240,255))
private fun fontColorMenu(){val opts=colorOptions();nexaBuilder().setTitle("Cor do texto").setItems(arrayOf("Padrão","Verde","Azul","Vermelho","Laranja","Branco","Amarelo","Azul claro")){_,which->snap();val s=book!!.sheets[book!!.active];val color=if(which==0)if(isDarkTheme)Color.rgb(239,248,242)else Color.rgb(35,48,41) else opts[which];for(r in grid.selStart..grid.selEnd)for(k in grid.selColStart..grid.selColEnd){val ce=s.cells[key(r,k)]?:Cell();ce.fontColor=color;s.cells[key(r,k)]=ce};grid.invalidate()}.show()}
private fun backgroundColorMenu(){val opts=colorOptions();nexaBuilder().setTitle("Cor de fundo").setItems(arrayOf("Sem preenchimento","Verde","Azul","Vermelho","Laranja","Branco","Amarelo","Azul claro")){_,which->snap();val s=book!!.sheets[book!!.active];val color=if(which==0)Color.WHITE else opts[which];for(r in grid.selStart..grid.selEnd)for(k in grid.selColStart..grid.selColEnd){val ce=s.cells[key(r,k)]?:Cell();ce.background=color;s.cells[key(r,k)]=ce};grid.invalidate()}.show()}
private fun numberFormatMenu(){nexaBuilder().setTitle("Formato numérico").setItems(arrayOf("Geral","Número","Moeda","Porcentagem","Data","Hora")){_,which->snap();val fmt=arrayOf("general","number","currency","percent","date","time")[which];val s=book!!.sheets[book!!.active];for(r in grid.selStart..grid.selEnd)for(k in grid.selColStart..grid.selColEnd){val ce=s.cells[key(r,k)]?:Cell();ce.numberFormat=fmt;s.cells[key(r,k)]=ce};grid.invalidate()}.show()}

 private fun fontSize(){val input=dialogInput();input.inputType=2;input.setText("14");nexaBuilder().setTitle("Tamanho da fonte").setView(input).setPositiveButton("Aplicar"){_,_->val n=input.text.toString().toFloatOrNull()?:14f;snap();val s=book!!.sheets[book!!.active];for(r in grid.selStart..grid.selEnd)for(k in grid.selColStart..grid.selColEnd){val ce=s.cells[key(r,k)]?:Cell();ce.fontSize=n.coerceIn(8f,72f);s.cells[key(r,k)]=ce};grid.invalidate()}.setNegativeButton("Cancelar",null).show()}
 private fun wrap(){snap();val s=book!!.sheets[book!!.active];for(r in grid.selStart..grid.selEnd)for(k in grid.selColStart..grid.selColEnd){val ce=s.cells[key(r,k)]?:Cell();ce.wrap=!ce.wrap;s.cells[key(r,k)]=ce};grid.invalidate()}
 private fun border(){snap();val s=book!!.sheets[book!!.active];val all=(grid.selStart..grid.selEnd).all{r->(grid.selColStart..grid.selColEnd).all{k->val ce=s.cells[key(r,k)];ce!=null&&ce.borderTop&&ce.borderRight&&ce.borderBottom&&ce.borderLeft}};for(r in grid.selStart..grid.selEnd)for(k in grid.selColStart..grid.selColEnd){val ce=s.cells[key(r,k)]?:Cell();ce.borderTop=!all;ce.borderRight=!all;ce.borderBottom=!all;ce.borderLeft=!all;s.cells[key(r,k)]=ce};grid.invalidate()}
 private fun conditional(){val s=book!!.sheets[book!!.active];val box=LinearLayout(this);box.orientation=LinearLayout.VERTICAL;val op=dialogSpinner(arrayOf("eq","neq","contains","gt","gte","lt","lte"));val v=dialogInput("Valor");box.addView(op);box.addView(v);nexaBuilder().setTitle("Formatação condicional").setView(box).setPositiveButton("Aplicar"){_,_->snap();s.rules.add(Rule(range(grid.selStart,grid.selColStart,grid.selEnd,grid.selColEnd),op.selectedItem.toString(),v.text.toString()));grid.invalidate()}.setNegativeButton("Cancelar",null).show()}
 private fun validation(){val s=book!!.sheets[book!!.active];val box=LinearLayout(this);box.orientation=LinearLayout.VERTICAL;val type=dialogSpinner(arrayOf("text","number","date","list"));val vals=dialogInput("Lista: A, B, C");val min=dialogInput("Mínimo");val max=dialogInput("Máximo");box.addView(type);box.addView(vals);box.addView(min);box.addView(max);nexaBuilder().setTitle("Validação").setView(box).setPositiveButton("Aplicar"){_,_->snap();val t=type.selectedItem.toString();val rule=Validation(t,vals.text.toString().split(",").map{it.trim()}.filter{it.isNotEmpty()},min.text.toString().toDoubleOrNull(),max.text.toString().toDoubleOrNull());for(r in grid.selStart..grid.selEnd)for(k in grid.selColStart..grid.selColEnd)s.validations[key(r,k)]=rule;grid.invalidate()}.setNegativeButton("Cancelar",null).show()}
 private fun groupRow(){val s=book!!.sheets[book!!.active];if(grid.selStart==grid.selEnd){showNexaToast("Selecione pelo menos 2 linhas para agrupar",NexaToastType.WARNING);return};val g=grid.selStart.toString()+":"+grid.selEnd.toString();if(s.rowGroups.any{rangeIndexOverlaps(it,g)}){showNexaToast("O intervalo de linhas sobrepõe outro grupo",NexaToastType.WARNING);return};snap();s.rowGroups.add(g);for(i in grid.selStart..grid.selEnd)s.groupedRows.add(i);grid.invalidate();showNexaToast("Linhas agrupadas",NexaToastType.SUCCESS)}
 private fun groupCol(){val s=book!!.sheets[book!!.active];if(grid.selColStart==grid.selColEnd){showNexaToast("Selecione pelo menos 2 colunas para agrupar",NexaToastType.WARNING);return};val g=grid.selColStart.toString()+":"+grid.selColEnd.toString();if(s.colGroups.any{rangeIndexOverlaps(it,g)}){showNexaToast("O intervalo de colunas sobrepõe outro grupo",NexaToastType.WARNING);return};snap();s.colGroups.add(g);for(i in grid.selColStart..grid.selColEnd)s.groupedCols.add(i);grid.invalidate();showNexaToast("Colunas agrupadas",NexaToastType.SUCCESS)}
 private fun toggleGroups(){val s=book!!.sheets[book!!.active];val rg=s.rowGroups.firstOrNull{rangeIndexContains(it,grid.selStart)};val cg=s.colGroups.firstOrNull{rangeIndexContains(it,grid.selColStart)};if(rg!=null){if(collapsedRows.contains(rg))collapsedRows.remove(rg)else collapsedRows.add(rg)};if(cg!=null){if(collapsedCols.contains(cg))collapsedCols.remove(cg)else collapsedCols.add(cg)};grid.invalidate()}
 private fun shiftIndexGroup(v:String,at:Int,d:Int):String{val b=rangeIndexBounds(v);return "${if(b.first>=at)b.first+d else b.first}:${if(b.second>=at)b.second+d else b.second}"}
private fun deleteIndexGroup(v:String,at:Int,d:Int):String?{val b=rangeIndexBounds(v);if(b.second<at)return v;if(b.first>=at+d)return "${b.first-d}:${b.second-d}";if(b.first>=at&&b.second<at+d)return null;val lo=if(b.first>=at)at else b.first;val hi=if(b.second>=at+d)b.second-d else at-1;if(hi<lo)return null;return "$lo:$hi"}
private fun rangeIndexBounds(v:String):Pair<Int,Int>{val p=v.split(":");val a=p[0].toInt();val b=p.getOrElse(1){p[0]}.toInt();return minOf(a,b) to maxOf(a,b)}
 private fun rangeIndexContains(v:String,n:Int):Boolean{val b=rangeIndexBounds(v);return n in b.first..b.second}
 private fun rangeIndexOverlaps(a:String,b:String):Boolean{val x=rangeIndexBounds(a);val y=rangeIndexBounds(b);return x.first<=y.second&&y.first<=x.second}
 private fun range(r1:Int,c1:Int,r2:Int,c2:Int)=key(minOf(r1,r2),minOf(c1,c2))+":"+key(maxOf(r1,r2),maxOf(c1,c2))
 private fun valid(v:String,r:Validation?):Boolean{if(r==null)return true;return when(r.type){"text"->true;"number"->v.toDoubleOrNull()?.let{x->(r.min==null||x>=r.min!!) && (r.max==null||x<=r.max!!)}?:false;"date"->try{val d=java.text.SimpleDateFormat("yyyy-MM-dd").parse(v)?:return false;val t=d.time;(r.min==null||t>=r.min!!) && (r.max==null||t<=r.max!!)}catch(_:Exception){false};"list"->r.values.contains(v);else->true}}
 private fun numberFormat(){snap();val s=book!!.sheets[book!!.active];for(r in grid.selStart..grid.selEnd)for(k in grid.selColStart..grid.selColEnd){val ce=s.cells[key(r,k)]?:Cell();ce.numberFormat=when(ce.numberFormat){"general"->"number";"number"->"currency";"currency"->"percent";else->"general"};s.cells[key(r,k)]=ce};grid.invalidate()}
 private fun autoFill(){val s=book!!.sheets[book!!.active];val r1=grid.selStart;val r2=grid.selEnd;val c1=grid.selColStart;val c2=grid.selColEnd;if(r1==r2&&c1==c2){showNexaToast("Selecione um intervalo para preencher",NexaToastType.WARNING);return};val seed=s.cells[key(r1,c1)]?.input?:"";if(seed.isBlank()){showNexaToast("A célula inicial está vazia",NexaToastType.WARNING);return};val seed2=s.cells[key(r1,minOf(c1+1,c2))]?.input?:"";val n1=seed.replace(",",".").toDoubleOrNull();val n2=seed2.replace(",",".").toDoubleOrNull();val step=if(n1!=null&&n2!=null)n2-n1 else null;snap();for(r in r1..r2)for(k in c1..c2){if(r==r1&&k==c1)continue;val dr=r-r1;val dc=k-c1;val v=when{seed.startsWith("=")->shiftFormula(seed,dr,dc);step!=null->formatSeriesNumber(n1!!+step*(if(c2>c1)dc else dr));else->seed};val old=s.cells[key(r,k)];s.cells[key(r,k)]=old?.copy(input=v)?:Cell(v)};grid.invalidate();showNexaToast("Preenchimento automático aplicado",NexaToastType.SUCCESS)}
private fun formatSeriesNumber(v:Double):String{if(v.isNaN()||v.isInfinite())return v.toString();return if(kotlin.math.abs(v-kotlin.math.round(v))<0.0000001)kotlin.math.round(v).toLong().toString() else String.format(Locale.US,"%.10f",v).trimEnd('0').trimEnd('.')}
 private fun shiftFormula(f:String,dr:Int,dc:Int):String=f.replace(Regex("(\\$?)([A-Z]+)(\\$?)([0-9]+)")){m->var n=0;for(ch in m.groupValues[2])n=n*26+ch.code-64;val ac=m.groupValues[1]=="$";val ar=m.groupValues[3]=="$";val rr=m.groupValues[4].toInt()+if(ar)0 else dr;val cc=n-1+if(ac)0 else dc;if(rr<1||cc<0)m.value else (if(ac)"$" else "")+col(cc)+(if(ar)"$" else "")+rr}
 private var internalClipboard:List<List<Cell?>>?=null;private var internalClipboardRow=0;private var internalClipboardCol=0
private fun copy(){val s=book!!.sheets[book!!.active];internalClipboardRow=grid.selStart;internalClipboardCol=grid.selColStart;internalClipboard=(grid.selStart..grid.selEnd).map{r->(grid.selColStart..grid.selColEnd).map{k->s.cells[key(r,k)]?.copy()}};val rows=internalClipboard!!.map{row->row.joinToString("\t"){it?.input?:""}};val cm=getSystemService(CLIPBOARD_SERVICE) as ClipboardManager;cm.setPrimaryClip(ClipData.newPlainText("Nexa",rows.joinToString("\n")));showNexaToast("Seleção copiada",NexaToastType.SUCCESS)}
private fun cut(){copy();snap();val s=book!!.sheets[book!!.active];for(r in grid.selStart..grid.selEnd)for(k in grid.selColStart..grid.selColEnd)s.cells.remove(key(r,k));grid.invalidate();showNexaToast("Seleção recortada",NexaToastType.SUCCESS)}
 private fun paste(){val s=book!!.sheets[book!!.active];val block=internalClipboard;if(block!=null){val rows=block.size;val cols=block.maxOfOrNull{it.size}?:0;if(rows>0&&cols>0&&grid.selStart+rows<=200&&grid.selColStart+cols<=50){snap();for((dr,row) in block.withIndex())for((dc,cell) in row.withIndex())if(cell!=null){val shifted=if(cell.input.startsWith("="))shiftFormula(cell.input,grid.selStart+dr-internalClipboardRow,grid.selColStart+dc-internalClipboardCol)else cell.input;s.cells[key(grid.selStart+dr,grid.selColStart+dc)]=cell.copy(input=shifted)}grid.invalidate();showNexaToast("Colado na seleção",NexaToastType.SUCCESS);return}};val cm=getSystemService(CLIPBOARD_SERVICE) as ClipboardManager;val v=cm.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString()?:return;val rows=v.replace("\r\n","\n").replace("\r","\n").split("\n").filter{it.isNotEmpty()}.map{it.split("\t")};if(rows.isEmpty())return;if(grid.selStart+rows.size>200||grid.selColStart+(rows.maxOf{it.size})>50){showNexaToast("A seleção excede o limite da planilha",NexaToastType.WARNING);return};snap();for((dr,row) in rows.withIndex())for((dc,value) in row.withIndex())s.cells[key(grid.selStart+dr,grid.selColStart+dc)]=s.cells[key(grid.selStart+dr,grid.selColStart+dc)]?.copy(input=value)?:Cell(value);grid.invalidate();showNexaToast("Colado na seleção",NexaToastType.SUCCESS)}
 private fun importFile(){startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE),PICK)}
 private fun exportFile(){val opts=arrayOf("Nexa (.nexa)","Excel (.xlsx)","CSV (.csv)","TSV (.tsv)");nexaBuilder().setTitle("Exportar").setItems(opts){_,which->val ext=when(which){0->"nexa";1->"xlsx";2->"csv";else->"tsv"};val mime=if(ext=="xlsx")"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" else "text/plain";startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).setType(mime).putExtra(Intent.EXTRA_TITLE,(book!!.name.ifBlank{"nexa"})+"."+ext),SAVE+which)}.show()}
 override fun onActivityResult(q:Int,res:Int,data:Intent?){super.onActivityResult(q,res,data);if(res!=RESULT_OK||data?.data==null)return;try{if(q==PICK){val uri=data.data!!;val name=(uri.lastPathSegment?:"").lowercase();if(name.endsWith(".xlsx")||name.contains("xlsx"))book=fromXlsx(contentResolver.openInputStream(uri)!!.readBytes())else{val raw=contentResolver.openInputStream(uri)?.bufferedReader()?.use{it.readText()}?:return;book=if(raw.trimStart().startsWith("{"))from(JSONObject(raw))else fromDelimited(raw,if(name.contains("tsv"))"\t" else ",")};grid.invalidate()}else{val ext=when(q){SAVE->"nexa";SAVE+1->"xlsx";SAVE+2->"csv";else->"tsv"};val out=contentResolver.openOutputStream(data.data!!)?:return;when(ext){"nexa"->out.write(toJson(book!!).toString(2).toByteArray());"xlsx"->out.write(toXlsx(book!!));else->out.write(delimited(book!!.sheets[book!!.active],if(ext=="csv")"," else "\t").toByteArray())};out.close();showNexaToast("Arquivo exportado",NexaToastType.SUCCESS)}}catch(_:Exception){showNexaToast("Arquivo inválido ou incompatível",NexaToastType.ERROR)}}
 private fun xmlEscape(v:String):String=v.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;")
 private fun xmlUnescape(v:String):String=v.replace("&lt;","<").replace("&gt;",">").replace("&quot;","\"").replace("&apos;","'").replace("&amp;","&")
 private fun xlsxCol(ref:String):Int{var n=0;for(ch in ref.takeWhile{it.isLetter()}.uppercase())n=n*26+ch.code-64;return n-1}
 private fun xlsxCellRef(r:Int,c:Int)=col(c)+(r+1)
 private fun toXlsx(b:Book):ByteArray{
  val out=ByteArrayOutputStream();val zip=java.util.zip.ZipOutputStream(out)
  fun entry(path:String,text:String){zip.putNextEntry(java.util.zip.ZipEntry(path));zip.write(text.toByteArray(Charsets.UTF_8));zip.closeEntry()}
  val sheets=b.sheets
  entry("[Content_Types].xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"+sheets.indices.joinToString(""){i->"<Override PartName=\"/xl/worksheets/sheet"+(i+1)+".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"}+"</Types>")
  entry("_rels/.rels","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>")
  entry("xl/_rels/workbook.xml.rels","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"+sheets.indices.joinToString(""){i->"<Relationship Id=\"rId"+(i+1)+"\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet"+(i+1)+".xml\"/>"}+"</Relationships>")
  entry("xl/workbook.xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>"+sheets.indices.joinToString(""){i->"<sheet name=\""+xmlEscape(sheets[i].name.take(31))+"\" sheetId=\""+(i+1)+"\" r:id=\"rId"+(i+1)+"\"/>"}+"</sheets></workbook>")
  for((si,s) in sheets.withIndex()){val rows=(0 until 200).mapNotNull{rr->val cells=(0 until 50).mapNotNull{cc->s.cells[key(rr,cc)]?.let{cc to it}};if(cells.isEmpty())null else rr to cells};val body=rows.joinToString(""){pair->val rr=pair.first;val cells=pair.second;"<row r=\""+(rr+1)+"\">"+cells.joinToString(""){pair2->val cc=pair2.first;val ce=pair2.second;val v=xmlEscape(ce.input);"<c r=\""+xlsxCellRef(rr,cc)+"\" t=\"inlineStr\"><is><t xml:space=\"preserve\">"+v+"</t></is></c>"}+"</row>"};entry("xl/worksheets/sheet"+(si+1)+".xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>"+body+"</sheetData></worksheet>")}
  zip.finish();zip.close();return out.toByteArray()
 }
 private fun fromXlsx(bytes:ByteArray):Book{
  val zip=java.util.zip.ZipInputStream(ByteArrayInputStream(bytes));val parts=mutableMapOf<String,String>();var e=zip.nextEntry
  while(e!=null){if(!e.isDirectory){parts[e.name]=zip.readBytes().toString(Charsets.UTF_8)};zip.closeEntry();e=zip.nextEntry};zip.close()
  val wb=parts["xl/workbook.xml"]?:throw Exception("xlsx workbook");val rel=parts["xl/_rels/workbook.xml.rels"]?:"";val rels=Regex("<Relationship[^>]*Id=\"([^\"]+)\"[^>]*Target=\"([^\"]+)\"").findAll(rel).associate{it.groupValues[1] to "xl/"+it.groupValues[2].removePrefix("/")};val ss=mutableListOf<Sheet>()
  Regex("<sheet\\b([^>]*)/>").findAll(wb).forEachIndexed{idx,m->val attrs=m.groupValues[1];val name=Regex("name=\"([^\"]*)\"").find(attrs)?.groupValues?.get(1)?.let(::xmlUnescape)?:"Planilha "+(idx+1);val rid=Regex("r:id=\"([^\"]+)\"").find(attrs)?.groupValues?.get(1)?:"rId"+(idx+1);val path=rels[rid]?:return@forEachIndexed;val xml=parts[path]?:return@forEachIndexed;val s=Sheet(name=name);Regex("<row[^>]*r=\"(\\\\d+)\"[^>]*>(.*?)</row>",RegexOption.DOT_MATCHES_ALL).findAll(xml).forEach{rm->Regex("<c[^>]*r=\"([A-Z]+)(\\\\d+)\"[^>]*>(.*?)</c>",RegexOption.DOT_MATCHES_ALL).findAll(rm.groupValues[2]).forEach{cm->val cc=xlsxCol(cm.groupValues[1]);val rr=cm.groupValues[2].toInt()-1;val body=cm.groupValues[3];val v=Regex("<t[^>]*>(.*?)</t>",RegexOption.DOT_MATCHES_ALL).find(body)?.groupValues?.get(1)?.let(::xmlUnescape)?:Regex("<v>(.*?)</v>",RegexOption.DOT_MATCHES_ALL).find(body)?.groupValues?.get(1)?:"";if(v.isNotEmpty())s.cells[key(rr,cc)]=Cell(v)}};ss.add(s)};if(ss.isEmpty())ss.add(Sheet(name="Planilha 1"));return Book(sheets=ss)
 }
 private fun delimited(s:Sheet,d:String):String{val maxR=(s.cells.keys.mapNotNull{Regex("[A-Z]+([0-9]+)").matchEntire(it)?.groupValues?.get(1)?.toIntOrNull()}.maxOrNull()?:1);val maxC=(s.cells.keys.mapNotNull{Regex("([A-Z]+)[0-9]+").matchEntire(it)?.groupValues?.get(1)?.let{v->var n=0;for(ch in v)n=n*26+ch.code-64;n}}.maxOrNull()?:1);return (0 until maxR).joinToString("\n"){r->(0 until maxC).joinToString(d){c->s.cells[key(r,c)]?.input?.replace(d," ")?.replace("\n"," ")?:""}}}
 private fun fromDelimited(raw:String,d:String):Book{val rows=raw.replace("\r\n","\n").replace("\r","\n").split("\n");val s=Sheet(name="Planilha 1");for((r,line) in rows.withIndex())for((c,v) in line.split(d).withIndex())if(v.isNotEmpty())s.cells[key(r,c)]=Cell(v);return Book(sheets=mutableListOf(s))}
 private fun merge(){val s=book!!.sheets[book!!.active];val r=range(grid.selStart,grid.selColStart,grid.selEnd,grid.selColEnd);if(grid.selStart==grid.selEnd&&grid.selColStart==grid.selColEnd){showNexaToast("Selecione mais de uma célula",NexaToastType.WARNING);return};if(s.merged.any{rangeOverlaps(it,r)}){showNexaToast("O intervalo sobrepõe uma mesclagem existente",NexaToastType.WARNING);return};snap();val anchor=key(grid.selStart,grid.selColStart);val value=s.cells[anchor]?.input?:"";for(rr in grid.selStart..grid.selEnd)for(cc in grid.selColStart..grid.selColEnd)if(key(rr,cc)!=anchor)s.cells.remove(key(rr,cc));if(value.isNotBlank())s.cells[anchor]=s.cells[anchor]?.copy(input=value)?:Cell(value);s.merged.add(r);grid.invalidate();showNexaToast("Células mescladas",NexaToastType.SUCCESS)}
 private fun rangeOverlaps(a:String,b:String):Boolean{fun bounds(x:String):IntArray{val p=x.split(":");val u=parseCellRef(p[0]);val v=parseCellRef(p.getOrElse(1){p[0]});return intArrayOf(minOf(u.first,v.first),minOf(u.second,v.second),maxOf(u.first,v.first),maxOf(u.second,v.second))};val x=bounds(a);val y=bounds(b);return x[0]<=y[2]&&y[0]<=x[2]&&x[1]<=y[3]&&y[1]<=x[3]}
private fun unmerge(){val s=book!!.sheets[book!!.active];val selected=range(grid.selStart,grid.selColStart,grid.selEnd,grid.selColEnd);val targets=s.merged.filter{it==selected||rangeOverlaps(it,selected)};if(targets.isEmpty()){showNexaToast("Nenhuma mesclagem nesta seleção",NexaToastType.INFO);return};snap();s.merged.removeAll(targets.toSet());grid.invalidate();showNexaToast("Mesclagem removida",NexaToastType.SUCCESS)}
 private fun freeze(){snap();val s=book!!.sheets[book!!.active];s.frozenRows=grid.selStart+1;s.frozenCols=grid.selColStart+1;grid.invalidate()}
 private fun hide(){snap();val s=book!!.sheets[book!!.active];for(i in grid.selStart..grid.selEnd)s.hiddenRows.add(i);for(i in grid.selColStart..grid.selColEnd)s.hiddenCols.add(i);grid.invalidate()}
 private fun show(){val s=book!!.sheets[book!!.active];val rows=(grid.selStart..grid.selEnd).toSet();val cols=(grid.selColStart..grid.selColEnd).toSet();snap();s.hiddenRows.removeAll(rows);s.hiddenCols.removeAll(cols);grid.invalidate();showNexaToast("Linhas/colunas exibidas",NexaToastType.SUCCESS)}
 private fun phase2(){
  val b=book?:return
  if(b.id==null){showNexaToast("Salve a planilha antes de usar as ferramentas da Fase 2.",NexaToastType.INFO);return}
  val items=arrayOf("Gráfico","Tabela","Tabela dinâmica","Dashboard","Comentário","Compartilhar","Nova versão","Analisar dados","Modelos","Sincronizar")
  nexaBuilder().setTitle("Nexa Completo").setItems(items){_,which->
   when(which){
    0->phase2Chart(b.id!!);1->phase2Table(b.id!!);2->phase2Pivot(b.id!!);3->phase2Dashboard(b.id!!);4->phase2Comment(b.id!!);5->phase2Share(b.id!!);6->phase2Version(b.id!!);7->phase2Analysis(b.id!!);8->phase2Templates();9->phase2Sync(b.id!!)
   }
  }.setNegativeButton("Fechar",null).show()
 }
 private fun phase2Range()=range(grid.selStart,grid.selColStart,grid.selEnd,grid.selColEnd)
 private fun phase2Chart(id:String){
  val box=LinearLayout(this);box.orientation=LinearLayout.VERTICAL
  val title=dialogInput("Título");box.addView(title)
  val type=Spinner(this);type.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,arrayOf("bar","line","area","pie","scatter"));box.addView(type)
  nexaBuilder().setTitle("Criar gráfico").setView(box).setPositiveButton("Criar"){_,_->Thread{try{val j=JSONObject().put("sheetId",book!!.sheets[book!!.active].id).put("title",title.text.toString().ifBlank{"Gráfico"}).put("type",type.selectedItem.toString()).put("range",phase2Range());val r=req("/v1/workbooks/$id/charts","POST",j.toString(),token);runOnUiThread{showNexaToast(if(r.first in 200..299)"Gráfico criado" else "Falha ao criar gráfico",if(r.first in 200..299)NexaToastType.SUCCESS else NexaToastType.ERROR)}}catch(_:Exception){runOnUiThread{showNexaToast("Falha de rede",NexaToastType.ERROR)}}}}.setNegativeButton("Cancelar",null).show()
 }
 private fun phase2Table(id:String){Thread{try{val j=JSONObject().put("sheetId",book!!.sheets[book!!.active].id).put("name","Tabela").put("range",phase2Range()).put("headerRow",true);val r=req("/v1/workbooks/$id/tables","POST",j.toString(),token);runOnUiThread{showNexaToast(if(r.first in 200..299)"Tabela criada" else "Falha ao criar tabela",if(r.first in 200..299)NexaToastType.SUCCESS else NexaToastType.ERROR)}}catch(_:Exception){runOnUiThread{showNexaToast("Falha de rede",NexaToastType.ERROR)}}}.start()}
 private fun phase2Pivot(id:String){
  val box=LinearLayout(this);box.orientation=LinearLayout.VERTICAL;val row=dialogInput("Campo de linha");val value=dialogInput("Campo de valor");val agg=dialogSpinner(arrayOf("sum","count","average","min","max"));box.addView(row);box.addView(value);box.addView(agg)
  nexaBuilder().setTitle("Tabela dinâmica").setView(box).setPositiveButton("Criar"){_,_->Thread{try{val j=JSONObject().put("sheetId",book!!.sheets[book!!.active].id).put("name","Tabela dinâmica").put("sourceRange",phase2Range()).put("rowField",row.text.toString()).put("valueField",value.text.toString()).put("aggregation",agg.selectedItem.toString());val r=req("/v1/workbooks/$id/pivots","POST",j.toString(),token);runOnUiThread{showNexaToast(if(r.first in 200..299)"Tabela dinâmica criada" else "Falha",if(r.first in 200..299)NexaToastType.SUCCESS else NexaToastType.ERROR)}}catch(_:Exception){runOnUiThread{showNexaToast("Falha de rede",NexaToastType.ERROR)}}}}.setNegativeButton("Cancelar",null).show()
 }
 private fun phase2Dashboard(id:String){Thread{try{val j=JSONObject().put("name","Dashboard").put("charts",JSONArray()).put("tables",JSONArray()).put("refreshMs",0);val r=req("/v1/workbooks/$id/dashboards","POST",j.toString(),token);runOnUiThread{showNexaToast(if(r.first in 200..299)"Dashboard criado" else "Falha",if(r.first in 200..299)NexaToastType.SUCCESS else NexaToastType.ERROR)}}catch(_:Exception){runOnUiThread{showNexaToast("Falha de rede",NexaToastType.ERROR)}}}.start()}
 private fun phase2Comment(id:String){
  val input=EditText(this);input.hint="Comentário"
  nexaBuilder().setTitle("Comentário em "+key(grid.selStart,grid.selColStart)).setView(input).setPositiveButton("Adicionar"){_,_->Thread{try{val j=JSONObject().put("sheetId",book!!.sheets[book!!.active].id).put("cell",key(grid.selStart,grid.selColStart)).put("body",input.text.toString());val r=req("/v1/workbooks/$id/comments","POST",j.toString(),token);runOnUiThread{showNexaToast(if(r.first in 200..299)"Comentário adicionado" else "Falha",if(r.first in 200..299)NexaToastType.SUCCESS else NexaToastType.ERROR)}}catch(_:Exception){runOnUiThread{showNexaToast("Falha de rede",NexaToastType.ERROR)}}}}.setNegativeButton("Cancelar",null).show()
 }
 private fun phase2Share(id:String){
  val box=LinearLayout(this);box.orientation=LinearLayout.VERTICAL;val email=EditText(this);email.hint="Email";val permission=Spinner(this);permission.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,arrayOf("viewer","commenter","editor"));box.addView(email);box.addView(permission)
  nexaBuilder().setTitle("Compartilhar").setView(box).setPositiveButton("Compartilhar"){_,_->Thread{try{val j=JSONObject().put("email",email.text.toString()).put("permission",permission.selectedItem.toString());val r=req("/v1/workbooks/$id/shares","POST",j.toString(),token);runOnUiThread{showNexaToast(if(r.first in 200..299)"Compartilhamento atualizado" else "Falha",if(r.first in 200..299)NexaToastType.SUCCESS else NexaToastType.ERROR)}}catch(_:Exception){runOnUiThread{showNexaToast("Falha de rede",NexaToastType.ERROR)}}}}.setNegativeButton("Cancelar",null).show()
 }
 private fun phase2Version(id:String){Thread{try{val r=req("/v1/workbooks/$id/versions","POST",JSONObject().put("label","Versão manual").put("source","manual").toString(),token);runOnUiThread{showNexaToast(if(r.first in 200..299)"Versão registrada" else "Falha",if(r.first in 200..299)NexaToastType.SUCCESS else NexaToastType.ERROR)}}catch(_:Exception){runOnUiThread{showNexaToast("Falha de rede",NexaToastType.ERROR)}}}.start()}
 private fun phase2Analysis(id:String){Thread{try{val j=JSONObject().put("sheetId",book!!.sheets[book!!.active].id).put("range",phase2Range()).put("limit",1000);val r=req("/v1/workbooks/$id/analysis","POST",j.toString(),token);runOnUiThread{nexaBuilder().setTitle("Análise de dados").setMessage(if(r.first in 200..299)r.second else "Falha ao analisar").setPositiveButton("OK",null).show()}}catch(_:Exception){runOnUiThread{showNexaToast("Falha de rede",NexaToastType.ERROR)}}}.start()}
  private fun phase2Templates(){templatesPage()}
 private fun phase2Sync(id:String){Thread{try{val j=JSONObject().put("sourceDevice","android-native").put("clientRevision",System.currentTimeMillis()).put("workbook",toJson(book!!));val r=req("/v1/workbooks/$id/sync","POST",j.toString(),token);runOnUiThread{showNexaToast(if(r.first in 200..299)"Sincronização concluída" else "Falha na sincronização",if(r.first in 200..299)NexaToastType.SUCCESS else NexaToastType.ERROR)}}catch(_:Exception){runOnUiThread{showNexaToast("Falha de rede",NexaToastType.ERROR)}}}.start()}
 private fun save(){val b=book?:return;Thread{try{val r=req(if(b.id==null)"/v1/workbooks" else "/v1/workbooks/"+b.id,if(b.id==null)"POST" else "PUT",toJson(b).toString(),token);if(r.first !in 200..299)throw Exception();if(b.id==null)book=from(JSONObject(r.second));runOnUiThread{showNexaToast("Salvo",NexaToastType.SUCCESS)}}catch(_:Exception){runOnUiThread{showNexaToast("Falha ao salvar",NexaToastType.ERROR)}}}.start()}
 private fun key(r:Int,c:Int)=col(c)+(r+1)
 private fun col(i:Int):String{var n=i+1;var z="";while(n>0){z=Char(65+(n-1)%26)+z;n=(n-1)/26};return z}
 private fun toJson(b:Book):JSONObject{val j=JSONObject().put("ownerId",uid).put("name",b.name).put("schemaVersion",4).put("activeSheetId",b.sheets[b.active].id);val sa=JSONArray();for(s in b.sheets){val o=JSONObject().put("id",s.id).put("name",s.name).put("frozenRows",s.frozenRows).put("frozenColumns",s.frozenCols);val cs=JSONObject();for((k,c) in s.cells)cs.put(k,JSONObject().put("input",c.input).put("style",JSONObject().put("bold",c.bold).put("italic",c.italic).put("underline",c.underline).put("strike",c.strike).put("align",c.align).put("numberFormat",c.numberFormat).put("fontSize",c.fontSize).put("fontColor",c.fontColor).put("backgroundColor",c.background).put("borderTop",c.borderTop).put("borderRight",c.borderRight).put("borderBottom",c.borderBottom).put("borderLeft",c.borderLeft).put("wrap",c.wrap).put("comment",c.comment)));val rs=JSONArray();for(rule in s.rules)rs.put(JSONObject().put("range",rule.range).put("op",rule.op).put("value",rule.value).put("bg",rule.bg).put("fg",rule.fg));val vs=JSONObject();for((k,v) in s.validations)vs.put(k,JSONObject().put("type",v.type).put("values",JSONArray(v.values)).put("min",v.min).put("max",v.max));val cws=JSONObject();for((k,v) in s.columnWidths)cws.put(k.toString(),v);val rhs=JSONObject();for((k,v) in s.rowHeights)rhs.put(k.toString(),v);o.put("conditionalRules",rs).put("validationRules",vs).put("cells",cs).put("columnWidths",cws).put("rowHeights",rhs).put("hiddenRows",JSONArray(s.hiddenRows.toList())).put("hiddenColumns",JSONArray(s.hiddenCols.toList())).put("mergedRanges",JSONArray(s.merged.toList())).put("groupedRows",JSONArray(s.groupedRows.toList())).put("groupedColumns",JSONArray(s.groupedCols.toList())).put("rowGroups",JSONArray(s.rowGroups)).put("columnGroups",JSONArray(s.colGroups));sa.put(o)};return j.put("sheets",sa)}
 private fun from(j:JSONObject):Book{
  val a=j.optJSONArray("sheets")?:JSONArray();val ss=mutableListOf<Sheet>()
  for(i in 0 until a.length()){
   val o=a.getJSONObject(i);val s=Sheet(o.optString("id",UUID.randomUUID().toString()),o.optString("name","Planilha "+(i+1)))
   val cs=o.optJSONObject("cells")?:JSONObject()
   for(k in cs.keys()){val c=cs.getJSONObject(k);val st=c.optJSONObject("style");s.cells[k]=Cell(c.optString("input"),st?.optBoolean("bold")?:false,st?.optBoolean("italic")?:false,st?.optBoolean("underline")?:false,st?.optBoolean("strike")?:false,st?.optInt("align")?:0,st?.optString("numberFormat","general")?:"general",st?.optDouble("fontSize",14.0)?.toFloat()?:14f,st?.optInt("fontColor",Color.DKGRAY)?:Color.DKGRAY,st?.optInt("backgroundColor",Color.WHITE)?:Color.WHITE,st?.optBoolean("borderTop")?:false,st?.optBoolean("borderRight")?:false,st?.optBoolean("borderBottom")?:false,st?.optBoolean("borderLeft")?:false,st?.optBoolean("wrap")?:false,st?.optString("comment","")?:"")}
   s.frozenRows=o.optInt("frozenRows");s.frozenCols=o.optInt("frozenColumns");o.optJSONObject("columnWidths")?.let{x->for(k in x.keys())s.columnWidths[k.toIntOrNull()?:0]=x.optDouble(k,130.0).toFloat()};o.optJSONObject("rowHeights")?.let{x->for(k in x.keys())s.rowHeights[k.toIntOrNull()?:0]=x.optDouble(k,52.0).toFloat()}
   o.optJSONArray("hiddenRows")?.let{x->for(n in 0 until x.length())s.hiddenRows.add(x.getInt(n))}
   o.optJSONArray("hiddenColumns")?.let{x->for(n in 0 until x.length())s.hiddenCols.add(x.getInt(n))}
   o.optJSONArray("mergedRanges")?.let{x->for(n in 0 until x.length())s.merged.add(x.getString(n))}
   o.optJSONArray("groupedRows")?.let{x->for(n in 0 until x.length())s.groupedRows.add(x.getInt(n))}
   o.optJSONArray("groupedColumns")?.let{x->for(n in 0 until x.length())s.groupedCols.add(x.getInt(n))}
   o.optJSONArray("rowGroups")?.let{x->for(n in 0 until x.length())s.rowGroups.add(x.getString(n))}
   o.optJSONArray("columnGroups")?.let{x->for(n in 0 until x.length())s.colGroups.add(x.getString(n))}
   o.optJSONArray("conditionalRules")?.let{x->for(n in 0 until x.length()){val q=x.getJSONObject(n);s.rules.add(Rule(q.optString("range"),q.optString("op"),q.optString("value"),q.optInt("bg",Color.rgb(22,77,49)),q.optInt("fg",Color.rgb(109,255,173))));}}
   o.optJSONObject("validationRules")?.let{x->for(k in x.keys()){val q=x.getJSONObject(k);val ar=q.optJSONArray("values")?:JSONArray();val vals=mutableListOf<String>();for(n in 0 until ar.length())vals.add(ar.getString(n));s.validations[k]=Validation(q.optString("type","text"),vals,if(q.has("min")&&!q.isNull("min"))q.optDouble("min") else null,if(q.has("max")&&!q.isNull("max"))q.optDouble("max") else null)}}
   ss.add(s)
  }
  if(ss.isEmpty())ss.add(Sheet(name="Planilha 1"));val id=j.optString("activeSheetId");val ai=ss.indexOfFirst{x->x.id==id};return Book(j.optString("_id").ifBlank{null},j.optString("name","Nova planilha"),ss,if(ai<0)0 else ai)
 }
 private fun req(path:String,method:String,body:String?,auth:String?):Pair<Int,String>{val c=URL(API+path).openConnection() as HttpURLConnection;c.requestMethod=method;c.connectTimeout=10000;c.readTimeout=15000;if(auth!=null)c.setRequestProperty("Authorization","Bearer "+auth);c.setRequestProperty("Content-Type","application/json");if(body!=null){c.doOutput=true;c.outputStream.use{it.write(body.toByteArray())}};val code=c.responseCode;val i=if(code>=400)c.errorStream else c.inputStream;return code to i.bufferedReader().use{it.readText()}}
 inner class Grid:View(this){var zoom=1f;var panX=0f;var panY=0f;var selStart=0;var selEnd=0;var selColStart=0;var selColEnd=0;var cw=130f;var rh=52f;val head=48f;val p=Paint(1)
   private fun colWidth(s:Sheet,k:Int)=s.columnWidths[k]?:cw
   private fun rowHeight(s:Sheet,r:Int)=s.rowHeights[r]?:rh
   private fun mergedBounds(v:String):IntArray{val p=v.split(":");val a=parse(p[0]);val b=parse(p.getOrElse(1){p[0]});return intArrayOf(minOf(a.first,b.first),minOf(a.second,b.second),maxOf(a.first,b.first),maxOf(a.second,b.second))}
   private fun mergedContains(v:String,r:Int,k:Int):Boolean{val b=mergedBounds(v);return r in b[0]..b[2]&&k in b[1]..b[3]}
   private fun mbWidth(s:Sheet,v:String):Float{val b=mergedBounds(v);var n=0f;for(k in b[1]..b[3])n+=colWidth(s,k);return n}
   private fun mbHeight(s:Sheet,v:String):Float{val b=mergedBounds(v);var n=0f;for(r in b[0]..b[2])n+=rowHeight(s,r);return n}
   private fun colOffset(s:Sheet,k:Int):Float{var x=0f;for(i in 0 until k)x+=colWidth(s,i);return x}
   private fun rowOffset(s:Sheet,r:Int):Float{var y=0f;for(i in 0 until r)y+=rowHeight(s,i);return y}
   override fun onDraw(c:Canvas){
    val s=book!!.sheets[book!!.active]
    c.save()
    c.scale(zoom,zoom)
    val dark=isDarkTheme
    val bg=if(dark)Color.rgb(10,16,13)else Color.rgb(248,250,249)
    val surface=if(dark)Color.rgb(20,29,24)else Color.WHITE
    val header=if(dark)Color.rgb(25,38,31)else Color.rgb(241,246,243)
    val headerStrong=if(dark)Color.rgb(30,48,38)else Color.rgb(232,240,235)
    val gridLine=if(dark)Color.rgb(48,65,55)else Color.rgb(218,226,221)
    val ink=if(dark)Color.rgb(235,246,239)else Color.rgb(35,48,41)
    val muted=if(dark)Color.rgb(150,172,158)else Color.rgb(101,116,108)
    val selectFill=if(dark)Color.rgb(25,65,44)else Color.rgb(226,244,234)
    val activeFill=if(dark)Color.rgb(34,91,60)else Color.rgb(213,239,223)
    val accent=if(dark)Color.rgb(82,232,139)else Color.rgb(19,122,84)
    c.drawColor(bg)
    p.style=Paint.Style.FILL
    fun cellX(k:Int)=head+colOffset(s,k)-if(k>=s.frozenCols)panX else 0f
    fun cellY(r:Int)=head+rowOffset(s,r)-if(r>=s.frozenRows)panY else 0f
    fun visible(r:Int,k:Int):Boolean{
      val rowCollapsed=s.rowGroups.any{collapsedRows.contains(it)&&rangeIndexContains(it,r)}
      val colCollapsed=s.colGroups.any{collapsedCols.contains(it)&&rangeIndexContains(it,k)}
      if(rowCollapsed||colCollapsed)return false
      val w=colWidth(s,k);val h=rowHeight(s,r)
      val x=cellX(k);val y=cellY(r)
      return x+w>=head&&x<=width/zoom&&y+h>=head&&y<=height/zoom
    }
    for(r in 0 until 200)for(k in 0 until 50){
      if(s.hiddenRows.contains(r)||s.hiddenCols.contains(k)||!visible(r,k))continue
      val x=cellX(k);val y=cellY(r);val baseW=colWidth(s,k);val baseH=rowHeight(s,r)
      val merge=s.merged.firstOrNull{mergedContains(it,r,k)}
      if(merge!=null){val mb=mergedBounds(merge);if(r!=mb[0]||k!=mb[1])continue}
      val w=if(merge==null)baseW else (mbWidth(s,merge));val h=if(merge==null)baseH else (mbHeight(s,merge))
      val selected=r in selStart..selEnd&&k in selColStart..selColEnd
      val active=r==selStart&&k==selColStart
      val ce=s.cells[key(r,k)]
      val base=if(r%2==1&&ce==null)if(dark)Color.rgb(18,27,22)else Color.rgb(250,252,251)else surface
      p.style=Paint.Style.FILL
      p.color=if(selected)selectFill else base
      c.drawRect(x,y,x+w,y+h,p)
      if(ce!=null){
        val rule=s.rules.firstOrNull{ruleMatch(ce.input,key(r,k),it)}
        p.color=if(selected)selectFill else if(ce.background==Color.WHITE)surface else (rule?.bg?:ce.background)
        c.drawRect(x+1,y+1,x+w-1,y+h-1,p)
        p.color=rule?.fg?:if(ce.fontColor==Color.DKGRAY)ink else ce.fontColor
        p.textSize=ce.fontSize.coerceIn(10f,28f)
        p.typeface=when{
          ce.bold&&ce.italic->Typeface.create(Typeface.DEFAULT,Typeface.BOLD_ITALIC)
          ce.bold->Typeface.DEFAULT_BOLD
          ce.italic->Typeface.create(Typeface.DEFAULT,Typeface.ITALIC)
          else->Typeface.DEFAULT
        }
        val tx=formatValue(showValue(ce.input,s),ce.numberFormat)
        val tw=p.measureText(tx)
        val txp=when(ce.align){1->x+(w-tw)/2f;2->x+w-tw-dp(8);else->x+dp(8)}
        val ty=y+h/2f-(p.ascent()+p.descent())/2f
        if(ce.wrap&&tw>w-dp(16)){p.textSize=(ce.fontSize-1f).coerceAtLeast(9f)}
        c.drawText(tx,txp,ty,p)
        if(ce.underline||ce.strike){
          p.style=Paint.Style.STROKE;p.strokeWidth=1.2f
          val ly=if(ce.strike)ty-p.textSize*0.32f else ty+1f
          c.drawLine(txp,ly,minOf(txp+p.measureText(tx),x+w-dp(5)),ly,p)
          p.style=Paint.Style.FILL
        }
        if(ce.borderTop||ce.borderRight||ce.borderBottom||ce.borderLeft){
          p.style=Paint.Style.STROKE;p.strokeWidth=1.5f;p.color=if(dark)Color.rgb(92,145,112)else Color.rgb(74,125,94)
          if(ce.borderTop)c.drawLine(x,y,x+w,y,p)
          if(ce.borderRight)c.drawLine(x+w,y,x+w,y+h,p)
          if(ce.borderBottom)c.drawLine(x,y+h,x+w,y+h,p)
          if(ce.borderLeft)c.drawLine(x,y,x,y+h,p)
          p.style=Paint.Style.FILL
        }
      }
      p.style=Paint.Style.STROKE;p.strokeWidth=1f;p.color=gridLine;c.drawRect(x,y,x+w,y+h,p);p.style=Paint.Style.FILL
      if(active){
        p.style=Paint.Style.STROKE;p.strokeWidth=2.5f;p.color=accent;c.drawRect(x+1.5f,y+1.5f,x+w-1.5f,y+h-1.5f,p)
        p.style=Paint.Style.FILL;p.color=accent;c.drawRoundRect(x+w-7f,y+h-7f,x+w-1f,y+h-1f,2f,2f,p);p.color=Color.WHITE;c.drawCircle(x+w-4f,y+h-4f,1.3f,p)
      }else if(selected){
        p.style=Paint.Style.STROKE;p.strokeWidth=1.8f;p.color=accent;c.drawRect(x+1f,y+1f,x+w-1f,y+h-1f,p);p.style=Paint.Style.FILL
      }
    }
    // Coluna/linha headers ficam fora da grade e acompanham a seleção.
    p.style=Paint.Style.FILL;p.color=header;c.drawRect(0f,0f,width/zoom,head,p);c.drawRect(0f,head,head,height/zoom,p)
    for(k in 0 until 50)if(!s.hiddenCols.contains(k)&&!s.colGroups.any{collapsedCols.contains(it)&&rangeIndexContains(it,k)}){
      val w=colWidth(s,k)
      val x=cellX(k);if(x+w<head||x>width/zoom)continue
      val selected=k in selColStart..selColEnd
      p.color=if(selected)headerStrong else header;p.style=Paint.Style.FILL;c.drawRect(x,0f,x+w,head,p)
      p.style=Paint.Style.STROKE;p.strokeWidth=1f;p.color=gridLine;c.drawRect(x,0f,x+w,head,p);p.style=Paint.Style.FILL
      p.color=if(selected)accent else muted;p.textSize=13f;p.typeface=if(selected)Typeface.DEFAULT_BOLD else Typeface.DEFAULT
      val label=col(k);val tw=p.measureText(label);c.drawText(label,x+(w-tw)/2f,head/2f-(p.ascent()+p.descent())/2f,p)
    }
    for(r in 0 until 200)if(!s.hiddenRows.contains(r)&&!s.rowGroups.any{collapsedRows.contains(it)&&rangeIndexContains(it,r)}){
      val h=rowHeight(s,r)
      val y=cellY(r);if(y+h<head||y>height/zoom)continue
      val selected=r in selStart..selEnd
      p.color=if(selected)headerStrong else header;p.style=Paint.Style.FILL;c.drawRect(0f,y,head,y+h,p)
      p.style=Paint.Style.STROKE;p.strokeWidth=1f;p.color=gridLine;c.drawRect(0f,y,head,y+h,p);p.style=Paint.Style.FILL
      p.color=if(selected)accent else muted;p.textSize=12.5f;p.typeface=if(selected)Typeface.DEFAULT_BOLD else Typeface.DEFAULT
      val label=(r+1).toString();val tw=p.measureText(label);c.drawText(label,head-dp(9)-tw,y+h/2f-(p.ascent()+p.descent())/2f,p)
    }
    // Canto superior esquerdo.
    p.style=Paint.Style.FILL;p.color=if(dark)Color.rgb(16,25,20)else Color.rgb(232,239,235);c.drawRect(0f,0f,head,head,p)
    p.style=Paint.Style.STROKE;p.strokeWidth=1f;p.color=gridLine;c.drawRect(0f,0f,head,head,p)
    p.style=Paint.Style.FILL;p.color=accent;c.drawCircle(head/2f,head/2f,5f,p)
    // Sombras discretas de painéis congelados.
    if(s.frozenCols>0){p.color=Color.argb(if(dark)70 else 35,0,0,0);c.drawRect(head+colOffset(s,s.frozenCols)-3f,head,head+colOffset(s,s.frozenCols)+3f,height/zoom,p)}
    if(s.frozenRows>0){p.color=Color.argb(if(dark)70 else 35,0,0,0);c.drawRect(head,head+rowOffset(s,s.frozenRows)-3f,width/zoom,head+rowOffset(s,s.frozenRows)+3f,p)}
    c.restore()
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
    private fun eat(ch:Char):Boolean{skip();if(pos<src.length&&src[pos]==ch){pos++;return true};return false}
    private fun eatText(t:String):Boolean{skip();if(src.regionMatches(pos,t,0,t.length,true)){pos+=t.length;return true};return false}
    fun parse():String{val v=comparison();skip();if(pos!=src.length)throw Exception("syntax");return formatNumber(v)}
    private fun comparison():Double{
      var a=additive()
      while(true){
       skip()
       val op=when{src.startsWith("<>",pos)->"<>";src.startsWith("<=",pos)->"<=";src.startsWith(">=",pos)->">=";src.startsWith("=",pos)->"=";src.startsWith("<",pos)->"<";src.startsWith(">",pos)->">";else->""}
       if(op.isBlank())return a
       pos+=op.length
       val b=additive()
       a=when(op){"="->if(a==b)1.0 else 0.0;"<>"->if(a!=b)1.0 else 0.0;"<"->if(a<b)1.0 else 0.0;">"->if(a>b)1.0 else 0.0;"<="->if(a<=b)1.0 else 0.0;">="->if(a>=b)1.0 else 0.0;else->0.0}
      }
    }
    private fun additive():Double{var v=term();while(true){if(eat('+'))v+=term()else if(eat('-'))v-=term()else return v}}
    private fun term():Double{var v=power();while(true){if(eat('*'))v*=power()else if(eat('/')){val d=power();if(d==0.0)throw Exception("DIV0");v/=d}else return v}}
    private fun power():Double{var v=unary();if(eat('^'))v=Math.pow(v,power());return v}
    private fun unary():Double{if(eat('+'))return unary();if(eat('-'))return -unary();return primary()}
    private fun primary():Double{
     skip()
     if(eat('(')){val v=comparison();if(!eat(')'))throw Exception("paren");return v}
     if(pos<src.length&&src[pos]=='"'){val s=quoted();return s.replace(",",".").toDoubleOrNull()?:0.0}
     val start=pos
     while(pos<src.length&&!src[pos].isWhitespace()&&!"+-*/%^(),<>=:".contains(src[pos]))pos++
     if(start==pos)throw Exception("token")
     val token=src.substring(start,pos)
     val num=token.replace(",",".").toDoubleOrNull()
     if(num!=null)return num
     skip()
     if(pos<src.length&&src[pos]=='(')return function(token)
     return resolve(token)
    }
    private fun quoted():String{if(!eat('"'))throw Exception("quote");val out=StringBuilder();while(pos<src.length){if(src[pos]=='"'){if(pos+1<src.length&&src[pos+1]=='"'){out.append('"');pos+=2}else{pos++;return out.toString()}}else{out.append(src[pos]);pos++}};throw Exception("quote")}
    private fun function(name0:String):Double{
      val name=name0.uppercase();if(!eat('('))throw Exception("function")
      val args=mutableListOf<String>();var depth=0;var start=pos;var closed=false
      while(pos<src.length){when(src[pos]){'"'->{pos++;while(pos<src.length&&src[pos]!='"')pos++;if(pos<src.length)pos++};'('->{depth++;pos++};')'->{if(depth==0){if(pos>start)args.add(src.substring(start,pos));pos++;closed=true;break};depth--;pos++};','->{if(depth==0){args.add(src.substring(start,pos));start=pos+1};pos++};else->pos++}}
      if(!closed)throw Exception("paren")
      fun arg(i:Int):Double=if(i<args.size)evalArg(args[i]) else 0.0
      fun vals(i:Int=0):List<String>=args.drop(i).flatMap{argumentValues(it)}
      return when(name){
       "SUM"->vals().mapNotNull{it.replace(",",".").toDoubleOrNull()}.sum()
       "AVERAGE","AVG"->{val n=vals().mapNotNull{it.replace(",",".").toDoubleOrNull()};if(n.isEmpty())0.0 else n.average()}
       "MIN"->{val n=vals().mapNotNull{it.replace(",",".").toDoubleOrNull()};n.minOrNull()?:0.0}
       "MAX"->{val n=vals().mapNotNull{it.replace(",",".").toDoubleOrNull()};n.maxOrNull()?:0.0}
       "COUNT"->vals().count{it.replace(",",".").toDoubleOrNull()!=null}.toDouble()
       "COUNTA"->vals().count{it.isNotBlank()}.toDouble()
       "ABS"->kotlin.math.abs(arg(0))
       "SQRT"->kotlin.math.sqrt(arg(0))
       "POWER"->Math.pow(arg(0),arg(1))
       "MOD"->{val b=arg(1);if(b==0.0)throw Exception("DIV0");arg(0)%b}
       "ROUND"->roundTo(arg(0),arg(1).toInt())
       "ROUNDUP"->roundDirected(arg(0),arg(1).toInt(),true)
       "ROUNDDOWN"->roundDirected(arg(0),arg(1).toInt(),false)
       "INT"->kotlin.math.floor(arg(0))
       "TRUNC"->roundDirected(arg(0),arg(1).toInt(),false)
       "IF"->if(arg(0)!=0.0)arg(1)else arg(2)
       "AND"->if(args.all{evalArg(it)!=0.0})1.0 else 0.0
       "OR"->if(args.any{evalArg(it)!=0.0})1.0 else 0.0
       "NOT"->if(arg(0)==0.0)1.0 else 0.0
       "COUNTIF"->countIf(args)
       "SUMIF"->sumIf(args)
       "PRODUCT"->vals().mapNotNull{it.replace(",",".").toDoubleOrNull()}.fold(1.0){a,b->a*b}
       "MEDIAN"->{val n=vals().mapNotNull{it.replace(",",".").toDoubleOrNull()}.sorted();if(n.isEmpty())0.0 else if(n.size%2==1)n[n.size/2] else (n[n.size/2-1]+n[n.size/2])/2}
       "LARGE"->{val n=vals(0).mapNotNull{it.replace(",",".").toDoubleOrNull()}.sortedDescending();n.getOrElse(arg(1).toInt()-1){0.0}}
       "SMALL"->{val n=vals(0).mapNotNull{it.replace(",",".").toDoubleOrNull()}.sorted();n.getOrElse(arg(1).toInt()-1){0.0}}
       else->throw Exception("function")
      }
    }
    private fun evalArg(arg:String):Double{val t=arg.trim();if(t.startsWith(""")&&t.endsWith("""))return t.substring(1,t.length-1).replace(",",".").toDoubleOrNull()?:0.0;return FormulaParser(t,sheet,seen).parse().replace(",",".").toDoubleOrNull()?:0.0}
    private fun countIf(args:List<String>):Double{if(args.size<2)throw Exception("args");val values=argumentValues(args[0]);val criteria=criteriaText(args[1]);return values.count{matchesCriteria(it,criteria)}.toDouble()}
    private fun sumIf(args:List<String>):Double{if(args.size<2)throw Exception("args");val criteriaVals=argumentValues(args[0]);val criteria=criteriaText(args[1]);val sumVals=if(args.size>2)argumentValues(args[2]) else criteriaVals;return criteriaVals.indices.filter{it<sumVals.size&&matchesCriteria(criteriaVals[it],criteria)}.sumOf{sumVals[it].replace(",",".").toDoubleOrNull()?:0.0}}
    private fun criteriaText(x:String):String{val t=x.trim();return if(t.startsWith(""")&&t.endsWith("""))t.substring(1,t.length-1)else t}
    private fun matchesCriteria(value:String,c:String):Boolean{
      val n=value.replace(",",".").toDoubleOrNull();val cn=c.replace(",",".").toDoubleOrNull()
      if(cn!=null)return n!=null&&n==cn
      val op=when{c.startsWith(">=")->">=";c.startsWith("<=")->"<=";c.startsWith("<>")->"<>";c.startsWith(">")->">";c.startsWith("<")->"<";c.startsWith("=")->"=";else->"contains"}
      val rhs=c.removePrefix(">=").removePrefix("<=").removePrefix("<>").removePrefix(">").removePrefix("<").removePrefix("=")
      val rn=rhs.replace(",",".").toDoubleOrNull()
      return if(rn!=null&&n!=null)when(op){">="->n>=rn;"<="->n<=rn;"<>"->n!=rn;">"->n>rn;"<"->n<rn;else->n==rn}else when(op){"="->value.equals(rhs,true);"<>"->!value.equals(rhs,true);else->value.contains(rhs,true)}
    }
    private fun roundTo(v:Double,d:Int):Double{val p=Math.pow(10.0,d.toDouble());return kotlin.math.round(v*p)/p}
    private fun roundDirected(v:Double,d:Int,up:Boolean):Double{val p=Math.pow(10.0,d.toDouble());return if(up)kotlin.math.ceil(v*p)/p else kotlin.math.floor(v*p)/p}
    private fun formatNumber(v:Double):String=if(v.isNaN()||v.isInfinite())v.toString()else if(kotlin.math.abs(v-kotlin.math.round(v))<1e-10)kotlin.math.round(v).toLong().toString()else String.format(Locale.US,"%.10f",v).trimEnd('0').trimEnd('.')
    private fun argumentValues(arg:String):List<String>{val t=arg.trim();val range=t.split(":");if(range.size==2){val a=cellPoint(range[0]);val b=cellPoint(range[1]);val out=mutableListOf<String>();for(r in minOf(a.first,b.first)..maxOf(a.first,b.first))for(c in minOf(a.second,b.second)..maxOf(a.second,b.second))out.add(resolveRaw(sheet,key(r,c)));return out};return listOf(resolveRaw(sheet,t))}
    private fun resolveRaw(s:Sheet,ref:String):String{val m=Regex("^(?:'((?:[^']|'')+)'|([A-Za-z0-9_ .-]+))!([A-Z]+[0-9]+)$").find(ref);if(m!=null){val name=(m.groupValues[1].ifBlank{m.groupValues[2]}).replace("''","'");val ts=book!!.sheets.firstOrNull{x->x.name==name}?:throw Exception("REF");return resolveCell(ts,m.groupValues[3].uppercase())};return if(Regex("^[A-Z]+[1-9][0-9]*$",RegexOption.IGNORE_CASE).matches(ref))resolveCell(s,ref.uppercase()) else ref}
    private fun resolve(ref:String):Double{val raw=resolveRaw(sheet,ref);return raw.replace(",",".").toDoubleOrNull()?:throw Exception("VALUE")}
    private fun resolveCell(s:Sheet,k:String):String{val id=s.id+"!"+k;if(!seen.add(id))throw Exception("CIRCULAR");try{val raw=s.cells[k]?.input?:"0";return if(raw.startsWith("="))eval(raw.substring(1),s,seen)else raw}finally{seen.remove(id)}}
    private fun cellPoint(k:String):Pair<Int,Int>{val m=Regex("([A-Z]+)([0-9]+)",RegexOption.IGNORE_CASE).find(k.trim())?:throw Exception("REF");var n=0;for(ch in m.groupValues[1].uppercase())n=n*26+ch.code-64;return Pair(m.groupValues[2].toInt()-1,n-1)}
   }
  private fun rangeOverlaps(a:String,b:String):Boolean{fun bounds(x:String):IntArray{val p=x.split(":");val u=parse(p[0]);val v=parse(p.getOrElse(1){p[0]});return intArrayOf(minOf(u.first,v.first),minOf(u.second,v.second),maxOf(u.first,v.first),maxOf(u.second,v.second))};val x=bounds(a);val y=bounds(b);return x[0]<=y[2]&&y[0]<=x[2]&&x[1]<=y[3]&&y[1]<=x[3]}
 private fun parse(x:String):Pair<Int,Int>{val m=Regex("([A-Z]+)([0-9]+)",RegexOption.IGNORE_CASE).find(x.trim())?:throw Exception();var n=0;for(ch in m.groupValues[1].uppercase())n=n*26+ch.code-64;return Pair(m.groupValues[2].toInt()-1,n-1)}
  private var lastX=0f;private var lastY=0f;private var panning=false;private var lastTapAt=0L;private var lastTapRow=-1;private var lastTapCol=-1
  override fun onTouchEvent(e:MotionEvent):Boolean{
   val x=e.x/zoom;val y=e.y/zoom
   when(e.action){
    MotionEvent.ACTION_DOWN->{commitFormulaEditor();lastX=x;lastY=y;panning=false;selStart=locR(y);selEnd=selStart;selColStart=locC(x);selColEnd=selColStart;syncFormulaEditor();invalidate();return true}
    MotionEvent.ACTION_MOVE->{if(e.pointerCount>=2){panning=true;panX=(panX-(x-lastX)).coerceAtLeast(0f);panY=(panY-(y-lastY)).coerceAtLeast(0f);lastX=x;lastY=y;invalidate();return true};selEnd=locR(y);selColEnd=locC(x);syncFormulaEditor();invalidate();return true}
    MotionEvent.ACTION_UP->{if(!panning&&y>=head){val now=System.currentTimeMillis();val row=selStart;val col=selColStart;val doubleTap=now-lastTapAt<350L&&row==lastTapRow&&col==lastTapCol;lastTapAt=now;lastTapRow=row;lastTapCol=col;syncFormulaEditor();if(doubleTap)edit(row,col)};return true}
   }
   return true
  }
  private fun locR(y:Float):Int{val s=book!!.sheets[book!!.active];val logical=if(y<head+gridFrozenHeight(s))y-head else y-head+panY;var acc=0f;for(r in 0 until 200){val h=rowHeight(s,r);if(logical<acc+h)return r;acc+=h};return 199}
  private fun locC(x:Float):Int{val s=book!!.sheets[book!!.active];val logical=if(x<head+gridFrozenWidth(s))x-head else x-head+panX;var acc=0f;for(k in 0 until 50){val w=colWidth(s,k);if(logical<acc+w)return k;acc+=w};return 49}
  private fun gridFrozenHeight(s:Sheet)=run{var v=0f;for(r in 0 until s.frozenRows)v+=rowHeight(s,r);v}
  private fun gridFrozenWidth(s:Sheet)=run{var v=0f;for(k in 0 until s.frozenCols)v+=colWidth(s,k);v}
 }
}