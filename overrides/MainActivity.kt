package ir.hafteh.app
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
private data class Event(val id:Long,val title:String,val rank:Int,val start:Int,val end:Int)
private val palette=listOf(Color(0xFF607D8B),Color(0xFF1565C0),Color(0xFFEF6C00),Color(0xFFC62828),Color(0xFF6A1B9A))
private val days=listOf("شنبه","یکشنبه","دوشنبه","سه‌شنبه","چهارشنبه","پنجشنبه","جمعه")
private fun dk(c:Calendar)=SimpleDateFormat("yyyy-MM-dd",Locale.US).format(c.time)
private fun weekStart(c:Calendar)=(c.clone() as Calendar).apply{add(Calendar.DAY_OF_YEAR,-((get(Calendar.DAY_OF_WEEK)+1)%7))}
private fun faDate(c:Calendar)=SimpleDateFormat("yyyy/MM/dd",Locale("fa","IR")).format(c.time)
class MainActivity:ComponentActivity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{Planner()}}}
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun Planner(){
 val ctx=androidx.compose.ui.platform.LocalContext.current
 val prefs=remember{ctx.getSharedPreferences("hafteh_data",0)}
 val data=remember{mutableStateMapOf<String,MutableList<Event>>().apply{runCatching{val o=JSONObject(prefs.getString("schedules","{}")?:"{}");o.keys().forEach{k->val a=o.getJSONArray(k);this[k]=MutableList(a.length()){i->val x=a.getJSONObject(i);val s=x.optInt("hour",9)*60+x.optInt("minute");Event(x.optLong("id",System.nanoTime()+i),x.optString("title"),x.optInt("rank",2),s,x.optInt("endHour",s/60+1)*60+x.optInt("endMinute",s%60))}}}}}
 fun save(){val o=JSONObject();data.forEach{(k,v)->val a=JSONArray();v.forEach{a.put(JSONObject().put("id",it.id).put("title",it.title).put("rank",it.rank).put("hour",it.start/60).put("minute",it.start%60).put("endHour",it.end/60).put("endMinute",it.end%60))};o.put(k,a)};prefs.edit().putString("schedules",o.toString()).apply()}
 var date by remember{mutableStateOf(Calendar.getInstance())};var dark by remember{mutableStateOf(prefs.getBoolean("dark",false))}
 var dialog by remember{mutableStateOf(false)};var editDate by remember{mutableStateOf("")};var editId by remember{mutableStateOf<Long?>(null)}
 var title by remember{mutableStateOf("")};var rank by remember{mutableIntStateOf(2)};var start by remember{mutableIntStateOf(540)};var end by remember{mutableIntStateOf(600)}
 var drag by remember{mutableStateOf<Gesture?>(null)}
 val first=weekStart(date);val dates=(0..6).map{(first.clone() as Calendar).apply{add(Calendar.DAY_OF_YEAR,it)}};val week=dates.map{data[dk(it)].orEmpty()}
 fun editor(d:Int,m:Int,e:Event?){editDate=dk(dates[d]);editId=e?.id;title=e?.title?:"";rank=e?.rank?:2;start=e?.start?:m;end=e?.end?:m+60;dialog=true}
 MaterialTheme(colorScheme=if(dark)darkColorScheme() else lightColorScheme()){
 Scaffold(topBar={TopAppBar(title={Text("هفته | برنامه‌ریز هفتگی")},actions={TextButton(onClick={dark=!dark;prefs.edit().putBoolean("dark",dark).apply()}){Text(if(dark)"روز" else "شب")}})},bottomBar={NavigationBar{NavigationBarItem(true,{},icon={Text("▦")},label={Text("برنامه هفتگی")});NavigationBarItem(false,{},icon={Text("▣")},label={Text("کپی برنامه")});NavigationBarItem(false,{dark=!dark},icon={Text("⚙")},label={Text("تنظیمات")})}}){pad->
 Column(Modifier.fillMaxSize().padding(pad).padding(8.dp)){
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){TextButton(onClick={date=(date.clone() as Calendar).apply{add(Calendar.WEEK_OF_YEAR,-1)}}){Text("هفته قبل")};Column{Text("${faDate(dates.first())} تا ${faDate(dates.last())}");TextButton(onClick={date=Calendar.getInstance()}){Text("امروز")}};TextButton(onClick={date=(date.clone() as Calendar).apply{add(Calendar.WEEK_OF_YEAR,1)}}){Text("هفته بعد")}}
 Text("لمس خانه خالی: افزودن | لمس بلوک: ویرایش | کشیدن بلوک: جابه‌جایی روز و ساعت",style=MaterialTheme.typography.bodySmall)
 WeeklyBoard(dates,week,drag,{d,m,e->editor(d,m,e)},{d,m,e->drag=Gesture(d,m,d,m,e)},{d,m->drag=drag?.copy(toDay=d,toMinute=m)},{val g=drag;if(g!=null){if(g.event==null){val a=minOf(g.minute,g.toMinute).coerceIn(420,1320);val b=maxOf(g.minute,g.toMinute).coerceIn(a+30,1410);editor(g.day,a,null);end=b}else{val ev=g.event;val old=dates.indexOfFirst{data[dk(it)].orEmpty().any{x->x.id==ev.id}};if(old>=0){val ns=(ev.start+g.toMinute-g.minute).coerceIn(420,1380);data[dk(dates[old])]=data[dk(dates[old])].orEmpty().filterNot{x->x.id==ev.id}.toMutableList();data[dk(dates[g.toDay])]=(data[dk(dates[g.toDay])].orEmpty()+ev.copy(start=ns,end=ns+ev.end-ev.start)).toMutableList();save()}}};drag=null},Modifier.weight(1f))
 }}
 if(dialog)AlertDialog(onDismissRequest={dialog=false},title={Text(if(editId==null)"افزودن برنامه" else "ویرایش برنامه")},text={Column(verticalArrangement=Arrangement.spacedBy(7.dp)){
 OutlinedTextField(title,{title=it},label={Text("عنوان")},singleLine=true);Text("رتبه اهمیت");Row{(1..5).forEach{r->Button(onClick={rank=r},modifier=Modifier.weight(1f),contentPadding=PaddingValues(0.dp),colors=ButtonDefaults.buttonColors(containerColor=if(rank==r)palette[r-1] else palette[r-1].copy(alpha=.4f))){Text("$r",color=Color.White)}}}
 Text("شروع ${"%02d:%02d".format(start/60,start%60)}");Stepper(start){start=it.coerceAtMost(end-30)};Text("پایان ${"%02d:%02d".format(end/60,end%60)}");Stepper(end){end=it.coerceAtLeast(start+30).coerceAtMost(1439)};Text(editDate,style=MaterialTheme.typography.labelSmall)
 }},confirmButton={TextButton(onClick={if(title.isNotBlank()&&end>start){val l=data[editDate].orEmpty().toMutableList();if(editId==null)l.add(Event(System.nanoTime(),title.trim(),rank,start,end))else{val i=l.indexOfFirst{it.id==editId};if(i>=0)l[i]=l[i].copy(title=title.trim(),rank=rank,start=start,end=end)};data[editDate]=l;save();dialog=false}}){Text("ذخیره")}},dismissButton={Row{if(editId!=null)TextButton(onClick={data[editDate]=data[editDate].orEmpty().filterNot{it.id==editId}.toMutableList();save();dialog=false}){Text("حذف",color=MaterialTheme.colorScheme.error)};TextButton(onClick={dialog=false}){Text("انصراف")}}})
 }}
}
private data class Gesture(val day:Int,val minute:Int,val toDay:Int,val toMinute:Int,val event:Event?)
@Composable private fun Stepper(v:Int,change:(Int)->Unit){Row{FilledTonalButton(onClick={change((v-30).coerceAtLeast(420))}){Text("−۳۰")};Text("%02d:%02d".format(v/60,v%60),Modifier.padding(horizontal=14.dp));FilledTonalButton(onClick={change((v+30).coerceAtMost(1410))}){Text("+۳۰")}}}
@Composable private fun WeeklyBoard(dates:List<Calendar>,events:List<List<Event>>,gesture:Gesture?,onTap:(Int,Int,Event?)->Unit,onStart:(Int,Int,Event?)->Unit,onMove:(Int,Int)->Unit,onFinish:()->Unit,modifier:Modifier){
 val line=MaterialTheme.colorScheme.outlineVariant;val scroll=rememberScrollState()
 BoxWithConstraints(modifier.fillMaxWidth()){val label=38.dp;val boardWidth=maxWidth-label
 Row(Modifier.fillMaxSize().horizontalScroll(scroll)){
 Canvas(Modifier.width(label).fillMaxHeight()){val rh=size.height/16f;for(i in 0..15)drawContext.canvas.nativeCanvas.drawText("${(i+7).toString().padStart(2,'0')}:00",0f,i*rh+rh*.6f,android.graphics.Paint().apply{color=android.graphics.Color.GRAY;textSize=20f})}
 Canvas(Modifier.width(boardWidth).fillMaxHeight().pointerInput(events,dates){detectTapGestures{p->val cw=size.width/7;val rh=size.height/16;val d=(p.x/cw).toInt().coerceIn(0,6);val m=(420+(p.y/rh).toInt()*60).coerceIn(420,1379);onTap(d,m,events[d].firstOrNull{m>=it.start&&m<it.end})}}.pointerInput(events,dates){detectDragGestures(onDragStart={p->val cw=size.width/7;val rh=size.height/16;val d=(p.x/cw).toInt().coerceIn(0,6);val m=(420+(p.y/rh).toInt()*60).coerceIn(420,1379);onStart(d,m,events[d].firstOrNull{m>=it.start&&m<it.end})},onDragEnd=onFinish){c,_->val cw=size.width/7;val rh=size.height/16;onMove((c.position.x/cw).toInt().coerceIn(0,6),(420+(c.position.y/rh).toInt()*60).coerceIn(420,1379));c.consume()}}){
 val cw=size.width/7;val rh=size.height/16
 for(d in 0..7)drawLine(line,Offset(d*cw,0f),Offset(d*cw,size.height),1f);for(h in 0..16)drawLine(line,Offset(0f,h*rh),Offset(size.width,h*rh),if(h%2==0)1.2f else .5f)
 for(d in 0..6){drawContext.canvas.nativeCanvas.drawText("${days[d]} ${SimpleDateFormat("dd",Locale("fa","IR")).format(dates[d].time)}",d*cw+3f,22f,android.graphics.Paint().apply{color=android.graphics.Color.DKGRAY;textSize=18f});events[d].forEach{e->val y=(e.start-420)/60f*rh;val h=((e.end-e.start)/60f*rh).coerceAtLeast(18f);drawRoundRect(palette[(e.rank-1).coerceIn(0,4)],Offset(d*cw+2,y.coerceAtLeast(26f)),Size(cw-4,h),androidx.compose.ui.geometry.CornerRadius(7f));drawContext.canvas.nativeCanvas.drawText(e.title.take(11),d*cw+5,y.coerceAtLeast(26f)+20f,android.graphics.Paint().apply{color=android.graphics.Color.WHITE;textSize=18f})}}
 gesture?.let{g->val left=minOf(g.day,g.toDay)*cw+2;val top=(minOf(g.minute,g.toMinute)-420)/60f*rh;val w=(kotlin.math.abs(g.toDay-g.day)+1)*cw-4;val h=(kotlin.math.abs(g.toMinute-g.minute)/60f*rh).coerceAtLeast(rh);drawRect(Color(0x554F46E5),Offset(left,top.coerceAtLeast(26f)),Size(w,h),style=Stroke(3f));drawContext.canvas.nativeCanvas.drawText("${minOf(g.minute,g.toMinute)/60}:00 - ${maxOf(g.minute,g.toMinute)/60}:00",left+4,top.coerceAtLeast(26f)+20,android.graphics.Paint().apply{color=android.graphics.Color.BLACK;textSize=18f})}
 }
 }}
}
