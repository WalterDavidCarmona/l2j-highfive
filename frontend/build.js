/**
 * build.js — Ofusca y minimiza los assets JS para producción.
 * Uso: node build.js
 *
 * Genera archivos .min.js en assets/js/dist/
 * index.html en producción debe apuntar a esos archivos.
 */
const fs   = require('fs');
const path = require('path');
const JavaScriptObfuscator = require('javascript-obfuscator');
const { minify } = require('terser');

const SRC_DIR  = path.join(__dirname, 'assets', 'js');
const DIST_DIR = path.join(__dirname, 'assets', 'js', 'dist');

if (!fs.existsSync(DIST_DIR)) fs.mkdirSync(DIST_DIR, { recursive: true });

const FILES = ['api.js', 'main.js'];

// Opciones de ofuscación — balance entre tamaño y protección
const OBFUSCATE_OPTIONS = {
  compact: true,
  controlFlowFlattening: true,
  controlFlowFlatteningThreshold: 0.4,
  deadCodeInjection: true,
  deadCodeInjectionThreshold: 0.2,
  debugProtection: true,           // rompe debugger del navegador
  debugProtectionInterval: 4000,   // re-activa cada 4s
  disableConsoleOutput: true,      // silencia console.log/warn/error
  identifierNamesGenerator: 'hexadecimal',
  log: false,
  numbersToExpressions: true,
  renameGlobals: false,            // false para no romper variables globales entre archivos
  selfDefending: true,             // el código se autoprotege si se formatea
  simplify: true,
  splitStrings: true,
  splitStringsChunkLength: 8,
  stringArray: true,
  stringArrayCallsTransform: true,
  stringArrayEncoding: ['base64'],
  stringArrayIndexShift: true,
  stringArrayRotate: true,
  stringArrayShuffle: true,
  stringArrayWrappersCount: 2,
  stringArrayWrappersChainedCalls: true,
  stringArrayWrappersParametersMaxCount: 4,
  stringArrayWrappersType: 'function',
  stringArrayThreshold: 0.75,
  transformObjectKeys: true,
  unicodeEscapeSequence: false,
};

async function build() {
  console.log('🔨 Iniciando build de ofuscación...\n');
  let totalOriginal = 0;
  let totalFinal = 0;

  for (const file of FILES) {
    const srcPath  = path.join(SRC_DIR, file);
    const distPath = path.join(DIST_DIR, file.replace('.js', '.min.js'));
    const source   = fs.readFileSync(srcPath, 'utf8');

    console.log(`📦 Procesando: ${file} (${(source.length/1024).toFixed(1)} KB)`);

    // Paso 1: Ofuscar
    const obfuscated = JavaScriptObfuscator.obfuscate(source, OBFUSCATE_OPTIONS).getObfuscatedCode();

    // Paso 2: Minimizar con Terser
    const minified = await minify(obfuscated, { compress: false, mangle: false });

    fs.writeFileSync(distPath, minified.code, 'utf8');

    const ratio = ((1 - minified.code.length / source.length) * 100).toFixed(0);
    console.log(`   ✅ → dist/${file.replace('.js','.min.js')} (${(minified.code.length/1024).toFixed(1)} KB, legibilidad reducida ~${ratio}%)`);

    totalOriginal += source.length;
    totalFinal    += minified.code.length;
  }

  console.log(`\n✨ Build completo. Total: ${(totalOriginal/1024).toFixed(1)} KB → ${(totalFinal/1024).toFixed(1)} KB`);
  console.log('\n📋 Recuerda actualizar index.html en producción:');
  console.log('   assets/js/api.js  →  assets/js/dist/api.min.js');
  console.log('   assets/js/main.js →  assets/js/dist/main.min.js');
}

build().catch(console.error);
