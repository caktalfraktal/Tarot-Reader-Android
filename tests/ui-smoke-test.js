const fs=require('fs'),vm=require('vm'),assert=require('assert');
const path=require('path');
const base=path.join(__dirname,'../android/app/src/main/assets/');
function element(){return {style:{},children:[],clientWidth:360,value:'',replaceChildren(){this.children=[]},append(e){this.children.push(e)},setAttribute(){},addEventListener(){},blur(){},showModal(){}}}
const elements={}; const storage={cast:JSON.stringify({cards:[0],reading:'Old reading',query:'Old query'})}; const classes=new Set();const context={console,JSON,Number,Math,Array,localStorage:{getItem(k){return storage[k]||null},setItem(k,v){storage[k]=v},removeItem(k){delete storage[k]}},window:{innerWidth:360,addEventListener(){}},document:{documentElement:{classList:{toggle(k,on){if(on)classes.add(k);else classes.delete(k)},contains(k){return classes.has(k)}}},getElementById(id){return elements[id]||(elements[id]=element())},querySelectorAll(){return []},createElement:element,activeElement:element()},Android:{reading(p,id){context.prompt=p},draw(){}}};vm.createContext(context);
vm.runInContext(fs.readFileSync(base+'deck.js','utf8')+fs.readFileSync(base+'prompts.js','utf8')+fs.readFileSync(base+'index.html','utf8').split('<script>')[1].split('</script>')[0],context);
assert.equal(storage.cast,undefined);
assert.equal(elements.spread.children[0].src,'Back.gif');
assert.equal(elements.reading.textContent,'');
vm.runInContext('toggleTheme()',context); assert(classes.has('dark'));assert.equal(storage.theme,'dark');
vm.runInContext('toggleTheme()',context); assert(!classes.has('dark'));assert.equal(storage.theme,'light');
console.log('Passed old-reading removal, initial card back, and direct dark/light toggles with saved theme preference.');
for(const width of [320,360,412,768,1000])for(const n of [1,3,10]){
 context.window.innerWidth=width;elements.spread.clientWidth=width;
 vm.runInContext('query="Test $& __CARD_0__";showDraw({cards:Array.from({length:'+n+'},(_,i)=>i),source:"Atmospheric Noise"})',context);
 assert(context.prompt.includes('Test $& __CARD_0__'));assert.equal(elements.spread.children.length,n);assert.equal(elements.meanings.children.length,n);
 for(const image of elements.spread.children){assert(parseFloat(image.style.left)>=0);assert(parseFloat(image.style.top)>=0);assert(parseFloat(image.style.top)+parseFloat(image.style.height)<=parseFloat(elements.spread.style.height)+1)}
 vm.runInContext('showReading({id:generation,text:"Test reading"})',context);assert(elements.reading.textContent.includes('Test reading'));
}
console.log('Passed all three spread layouts at five phone/tablet widths, exact query handling, card/meaning counts and reading callbacks.');
