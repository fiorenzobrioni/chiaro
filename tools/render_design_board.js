// Draws the frozen canvas boards of a design folder (`docs/design/in-immagini/`) as images,
// for the comparison the fidelity rule asks for (PLANNING, «Fase 17», la regola della fedeltà,
// point 3; done in 17.4). The boards are pages of the canvas's Design Component format, which do
// not open in a browser without its runtime: this is a minimal stand-in for that runtime, enough
// for them — the `{{holes}}`, `<sc-for>`, `<sc-if>` and the `renderVals()` of the script at the
// foot of each board — expanded into plain HTML and drawn by Chromium at twice the size, the
// density of the widget gallery (`InPicturesGallery`), so a card of each can be laid side by side
// at the same scale. Every element with the class `w` (a card) is also saved on its own.
//
//     node tools/render_design_board.js docs/design/in-immagini <out-dir> [Board ...]
//
// The boards default to Forms, Grounds and Anatomy. Needs Playwright with its Chromium.
const fs = require('fs');
const { chromium } = require('playwright');

function expand(src) {
  const helmet = (src.match(/<helmet>([\s\S]*?)<\/helmet>/) || [null, ''])[1];
  const body = src.match(/<x-dc>([\s\S]*?)<\/x-dc>/)[1].replace(/<helmet>[\s\S]*?<\/helmet>/, '');
  const script = src.match(/<script type="text\/x-dc"[^>]*>([\s\S]*?)<\/script>/)[1];
  const DCLogic = class { constructor(p) { this.props = p || {}; this.state = {}; } setState() {} };
  const vals = new Function('DCLogic', script + '\nreturn new Component({}).renderVals();')(DCLogic);
  function look(path, scope) {
    let cur = path.split('.')[0] in scope ? scope : vals;
    for (const k of path.split('.')) { if (cur == null) return ''; cur = cur[k]; }
    return cur;
  }
  function holes(s, scope) {
    return s.replace(/\{\{\s*([\w.]+)\s*\}\}/g, (_, p) => {
      const v = look(p, scope);
      return typeof v === 'function' ? '' : (v === undefined || v === null ? '' : v);
    });
  }
  function run(s, scope) {
    let out = '';
    for (;;) {
      const m = s.match(/<(sc-for|sc-if)\b([^>]*)>/);
      if (!m) return out + holes(s, scope);
      out += holes(s.slice(0, m.index), scope);
      const tag = m[1];
      let depth = 1;
      const re = new RegExp('<(/?)' + tag + '\\b[^>]*>', 'g');
      re.lastIndex = m.index + m[0].length;
      let end = -1, closeLen = 0;
      while (depth > 0) {
        const t = re.exec(s);
        if (!t) throw new Error('unclosed ' + tag);
        depth += t[1] ? -1 : 1;
        if (depth === 0) { end = t.index; closeLen = t[0].length; }
      }
      const inner = s.slice(m.index + m[0].length, end);
      const attrs = m[2];
      if (tag === 'sc-for') {
        const list = look(attrs.match(/list="\{\{\s*([\w.]+)\s*\}\}"/)[1], scope) || [];
        const as = attrs.match(/as="(\w+)"/)[1];
        for (const item of list) out += run(inner, Object.assign({}, scope, { [as]: item }));
      } else if (look(attrs.match(/value="\{\{\s*([\w.]+)\s*\}\}"/)[1], scope)) {
        out += run(inner, scope);
      }
      s = s.slice(end + closeLen);
    }
  }
  return '<!doctype html><html><head><meta charset="utf-8">' + helmet + '</head><body style="margin:0">' + run(body, {}) + '</body></html>';
}

(async () => {
  const srcDir = process.argv[2];
  const outDir = process.argv[3];
  const boards = process.argv.length > 4 ? process.argv.slice(4) : ['Forms', 'Grounds', 'Anatomy'];
  const browser = await chromium.launch();
  for (const name of boards) {
    const html = expand(fs.readFileSync(srcDir + '/' + name + '.dc.html', 'utf8'));
    fs.writeFileSync(outDir + '/' + name + '.html', html);
    const page = await browser.newPage({ deviceScaleFactor: 2, viewport: { width: 1800, height: 1400 } });
    await page.setContent(html, { waitUntil: 'load', timeout: 15000 }).catch(() => {});
    await page.waitForTimeout(500);
    const root = await page.$('body > div');
    await root.screenshot({ path: outDir + '/' + name + '.png' });
    const cards = await page.$$('.w');
    for (let k = 0; k < cards.length; k++) {
      await cards[k].screenshot({ path: outDir + '/' + name + '-card-' + k + '.png', omitBackground: true });
    }
    console.log(name, 'cards', cards.length);
    await page.close();
  }
  await browser.close();
})();
