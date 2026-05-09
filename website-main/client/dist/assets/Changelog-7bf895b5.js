import{r as l,j as e,d,L as x}from"./vendor-7e79a346.js";import{a as m}from"./axios-84503384.js";import{P as h}from"./PageWrapper-47087110.js";import{m as o}from"./framer-motion-d86ec07a.js";import{c as p,D as g}from"./index-5ad19771.js";import"./syntax-highlighter-128d5034.js";/**
 * @license lucide-react v1.14.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */const f=[["path",{d:"M15 3h6v6",key:"1q9fwt"}],["path",{d:"M10 14 21 3",key:"gplh6r"}],["path",{d:"M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6",key:"a6xqqp"}]],u=p("external-link",f),c=[{version:"1.0.0",tagName:"v1.0.0",releasedAt:new Date("2025-05-01"),isLatest:!0,githubUrl:"https://github.com",changelog:`## 🔴 Breaking Changes
- Initial release — no breaking changes

## ✅ New Features
- Unified service dashboard
- One-click start/stop for MySQL, PostgreSQL, Redis, Nginx, Tomcat
- Real-time CPU/RAM monitoring
- Log streaming with search and filter
- Task scheduler with visual Cron editor
- Docker integration
- JVM profiler via JMX
- Security scanner
- Plugin system via Java SPI
- Smart port conflict resolver
- Environment profiles (Dev/Staging/Prod)
- Keyboard command palette (Ctrl+P)

## 🐛 Bug Fixes
- N/A (initial release)

## ⚡ Performance
- Optimized service polling with configurable intervals
- Lazy-loaded dashboard panels`}];function b(i){if(!i)return null;const s=[];let r=[];const a=()=>{r.length>0&&(s.push(e.jsx("ul",{className:"space-y-1 list-disc list-inside mb-4",children:r},`ul-${s.length}`)),r=[])};return i.split(`
`).forEach((t,n)=>{t.startsWith("## ")?(a(),s.push(e.jsx("h3",{className:"text-lg font-semibold text-white mt-6 mb-3",children:t.replace("## ","")},n))):t.startsWith("### ")?(a(),s.push(e.jsx("h4",{className:"text-base font-semibold text-text-primary mt-4 mb-2",children:t.replace("### ","")},n))):t.startsWith("- ")?r.push(e.jsx("li",{className:"text-text-secondary",children:t.replace("- ","")},n)):t.trim()&&(a(),s.push(e.jsx("p",{className:"text-text-secondary mb-2",children:t},n)))}),a(),s}function k(){const[i,s]=l.useState([]),[r,a]=l.useState(!0);return l.useEffect(()=>{m.get("/api/releases").then(({data:t})=>{s(t.length?t:c),a(!1)}).catch(()=>{s(c),a(!1)})},[]),e.jsxs(h,{children:[e.jsxs(d,{children:[e.jsx("title",{children:"zenviX — Dev Environment Manager | Changelog"}),e.jsx("meta",{name:"description",content:"zenviX version history and release notes."})]}),e.jsxs("div",{className:"max-w-4xl mx-auto px-4 py-20",children:[e.jsxs(o.div,{initial:{opacity:0,y:20},animate:{opacity:1,y:0},className:"mb-16",children:[e.jsx("h1",{className:"text-5xl font-display font-bold text-white",children:"Changelog"}),e.jsx("p",{className:"mt-4 text-text-secondary",children:"All notable changes to zenviX."})]}),r?e.jsx("div",{className:"text-text-muted text-center py-20",children:"Loading releases..."}):e.jsxs("div",{className:"relative",children:[e.jsx("div",{className:"absolute left-4 top-0 bottom-0 w-px bg-border"}),e.jsx("div",{className:"space-y-12",children:i.map((t,n)=>e.jsxs(o.div,{initial:{opacity:0,x:-20},whileInView:{opacity:1,x:0},viewport:{once:!0},transition:{delay:n*.1},className:"relative pl-12",children:[e.jsx("div",{className:`absolute left-0 w-8 h-8 rounded-full border-2 flex items-center justify-center text-xs font-bold ${t.isLatest?"border-accent bg-accent/20 text-accent":"border-border bg-bg-card text-text-muted"}`,children:"v"}),e.jsxs("div",{className:"p-6 bg-bg-card border border-border rounded-lg",children:[e.jsxs("div",{className:"flex flex-wrap items-center gap-3 mb-4",children:[e.jsx("span",{className:"text-xl font-display font-bold text-white",children:t.tagName||`v${t.version}`}),t.isLatest&&e.jsx("span",{className:"px-2 py-0.5 bg-accent/10 border border-accent text-accent text-xs rounded font-mono",children:"Latest"}),t.releasedAt&&e.jsx("span",{className:"text-text-muted text-sm",children:new Date(t.releasedAt).toLocaleDateString("en-US",{month:"long",day:"numeric",year:"numeric"})})]}),e.jsx("div",{className:"prose prose-invert max-w-none",children:b(t.changelog)}),e.jsxs("div",{className:"mt-6 flex gap-3",children:[t.githubUrl&&e.jsxs("a",{href:t.githubUrl,target:"_blank",rel:"noopener noreferrer",className:"flex items-center gap-2 text-sm text-text-secondary hover:text-accent transition-colors",children:[e.jsx(u,{size:14}),"GitHub Release"]}),e.jsxs(x,{to:"/download",className:"flex items-center gap-2 text-sm text-text-secondary hover:text-accent transition-colors",children:[e.jsx(g,{size:14}),"Download"]})]})]})]},t.version))})]})]})]})}export{k as default};
