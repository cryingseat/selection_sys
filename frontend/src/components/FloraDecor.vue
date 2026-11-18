<script setup>
// 日系水彩风装饰元素：花瓣logo、水彩云、樱花枝、藤蔓、花簇分隔线、草地过渡带
// 统一用 span 单根包裹，保证 class/style 透传属性生效
defineProps({
  kind: { type: String, default: 'divider' },
  tint: { type: String, default: '#fce8ef' },
  size: { type: Number, default: 24 }
})
const uid = `f${Math.random().toString(36).slice(2, 8)}`
</script>

<template>
  <span class="fp-decor" aria-hidden="true">
    <!-- 五瓣花 logo（定制图标） -->
    <img v-if="kind === 'logo'" src="/images/flower-logo.png" :width="size" :height="size" alt="" />

    <!-- 首屏左侧漂移水彩云 -->
    <svg v-else-if="kind === 'cloud-l'" viewBox="0 0 420 180" fill="none">
      <defs>
        <linearGradient :id="`${uid}cl`" x1="0" y1="0" x2="420" y2="140" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stop-color="#fff5f8" stop-opacity=".95" />
          <stop offset="55%" stop-color="#f7faef" stop-opacity=".85" />
          <stop offset="100%" stop-color="#ffffff" stop-opacity="0" />
        </linearGradient>
      </defs>
      <path d="M-20 120C10 65 75 45 130 72C165 30 235 35 268 78C305 68 355 88 380 125C320 142 95 148 -20 120Z" :fill="`url(#${uid}cl)`" />
    </svg>

    <!-- 首屏右侧漂移水彩云 -->
    <svg v-else-if="kind === 'cloud-r'" viewBox="0 0 420 180" fill="none">
      <defs>
        <linearGradient :id="`${uid}cr`" x1="420" y1="0" x2="0" y2="140" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stop-color="#f2f7e4" stop-opacity=".95" />
          <stop offset="55%" stop-color="#fff4f7" stop-opacity=".85" />
          <stop offset="100%" stop-color="#ffffff" stop-opacity="0" />
        </linearGradient>
      </defs>
      <path d="M440 125C395 60 325 45 275 76C235 32 165 40 135 84C95 75 45 95 20 130C95 145 320 150 440 125Z" :fill="`url(#${uid}cr)`" />
    </svg>

    <!-- 流程区漂浮云（含玻璃方块） -->
    <svg v-else-if="kind === 'loop-cloud'" viewBox="0 0 360 160" fill="none">
      <path d="M-10 110C20 55 85 40 135 68C170 26 235 32 265 74C298 66 335 84 355 115C290 132 80 136 -10 110Z" fill="#f7eef2" opacity=".9" />
      <g>
        <rect x="28" y="42" width="54" height="54" rx="12" fill="rgba(255,255,255,.28)" stroke="rgba(255,255,255,.68)" />
        <rect x="88" y="86" width="47" height="47" rx="12" fill="rgba(255,255,255,.28)" stroke="rgba(255,255,255,.68)" />
        <rect x="146" y="34" width="43" height="43" rx="12" fill="rgba(255,255,255,.28)" stroke="rgba(255,255,255,.68)" />
      </g>
    </svg>

    <!-- 步骤面板小云朵 -->
    <svg v-else-if="kind === 'mini-cloud'" viewBox="0 0 180 76" fill="none">
      <path d="M10 56C24 28 58 20 84 35C102 14 138 18 152 40C168 38 176 48 172 58C136 66 42 66 10 56Z" :fill="tint" />
      <circle cx="136" cy="24" r="5" fill="#f29cb5" />
      <circle cx="146" cy="30" r="3.5" fill="#b9d458" />
    </svg>

    <!-- 草地野花过渡带 -->
    <svg v-else-if="kind === 'meadow'" viewBox="0 0 1440 90" preserveAspectRatio="none" fill="none">
      <path d="M0 90L0 58Q120 34 240 56T480 52T720 60T960 50T1200 58T1440 48L1440 90Z" fill="#eef4dc" opacity=".72" />
      <path d="M0 90L0 68Q180 48 360 66T720 64T1080 68T1440 62L1440 90Z" fill="#e2ecc5" opacity=".68" />
      <g stroke="#6d9612" stroke-width="2" stroke-linecap="round" opacity=".58">
        <path d="M38 90Q44 54 58 34M46 90Q38 60 26 44M52 90Q64 62 76 48" />
        <path d="M168 90Q176 50 192 28M175 90Q164 58 152 40" />
        <path d="M310 90Q318 56 332 36M316 90Q306 62 292 48" />
        <path d="M465 90Q472 52 488 30M474 90Q484 60 498 44" />
        <path d="M620 90Q612 56 596 36M628 90Q638 58 652 42" />
        <path d="M780 90Q788 50 804 28M786 90Q774 60 762 44" />
        <path d="M935 90Q944 54 960 34M942 90Q930 62 918 46" />
        <path d="M1090 90Q1098 52 1114 32M1096 90Q1084 58 1072 42" />
        <path d="M1245 90Q1254 54 1270 36M1252 90Q1240 62 1226 46" />
        <path d="M1380 90Q1372 52 1356 32M1388 90Q1398 58 1412 40" />
      </g>
      <g>
        <circle cx="58" cy="34" r="6" fill="#f5b8cb" />
        <circle cx="58" cy="34" r="2.2" fill="#fff" />
        <circle cx="76" cy="48" r="4.5" fill="#FADCE6" />
        <circle cx="192" cy="28" r="6.5" fill="#f29cb5" />
        <circle cx="152" cy="40" r="4.5" fill="#b9d458" />
        <circle cx="332" cy="36" r="5.5" fill="#f5b8cb" />
        <circle cx="488" cy="30" r="6.5" fill="#FADCE6" />
        <circle cx="488" cy="30" r="2.3" fill="#d96b8a" />
        <circle cx="596" cy="36" r="5.5" fill="#f29cb5" />
        <circle cx="804" cy="28" r="6.5" fill="#f5b8cb" />
        <circle cx="762" cy="44" r="4.5" fill="#b9d458" />
        <circle cx="960" cy="34" r="6" fill="#FADCE6" />
        <circle cx="1114" cy="32" r="6" fill="#f29cb5" />
        <circle cx="1270" cy="36" r="5.5" fill="#f5b8cb" />
        <circle cx="1356" cy="32" r="6.5" fill="#FADCE6" />
        <circle cx="1412" cy="40" r="4.8" fill="#f29cb5" />
      </g>
    </svg>

    <!-- 区块标题藤蔓分隔线 -->
    <svg v-else-if="kind === 'divider'" viewBox="0 0 280 34" fill="none">
      <path d="M12 18C65 10 115 24 140 17C165 10 215 24 268 16" stroke="#8aa844" stroke-width="1.5" stroke-linecap="round" opacity=".65" />
      <path d="M48 15C62 6 82 8 95 16M185 17C198 9 218 9 232 16" stroke="#b5c97c" stroke-width="1.2" stroke-linecap="round" opacity=".6" />
      <ellipse cx="64" cy="12" rx="8" ry="3.5" transform="rotate(-22 64 12)" fill="#b9d458" opacity=".8" />
      <ellipse cx="98" cy="21" rx="8" ry="3.4" transform="rotate(18 98 21)" fill="#6d9612" opacity=".55" />
      <ellipse cx="182" cy="21" rx="8" ry="3.4" transform="rotate(-18 182 21)" fill="#6d9612" opacity=".55" />
      <ellipse cx="216" cy="12" rx="8" ry="3.5" transform="rotate(22 216 12)" fill="#b9d458" opacity=".8" />
      <g transform="translate(140 17)">
        <circle r="7.5" fill="#fce4ec" />
        <path d="M0-8C2.4-8 3.6-5 0-1C-3.6-5-2.4-8 0-8Z" fill="#f29cb5" />
        <path d="M0-8C2.4-8 3.6-5 0-1C-3.6-5-2.4-8 0-8Z" fill="#f5b8cb" transform="rotate(72)" />
        <path d="M0-8C2.4-8 3.6-5 0-1C-3.6-5-2.4-8 0-8Z" fill="#f29cb5" transform="rotate(144)" />
        <path d="M0-8C2.4-8 3.6-5 0-1C-3.6-5-2.4-8 0-8Z" fill="#f5b8cb" transform="rotate(216)" />
        <path d="M0-8C2.4-8 3.6-5 0-1C-3.6-5-2.4-8 0-8Z" fill="#ee8ca9" transform="rotate(288)" />
        <circle r="2.2" fill="#4a6714" />
      </g>
      <circle cx="42" cy="16" r="4" fill="#f5b8cb" opacity=".85" />
      <circle cx="115" cy="13" r="3.2" fill="#f7c6d6" opacity=".8" />
      <circle cx="165" cy="13" r="3.2" fill="#f7c6d6" opacity=".8" />
      <circle cx="238" cy="16" r="4" fill="#f5b8cb" opacity=".85" />
    </svg>

    <!-- 左上樱花枝 -->
    <svg v-else-if="kind === 'sprig-tl'" viewBox="0 0 320 180" fill="none">
      <path d="M-10 35C55 42 125 68 205 118M75 52C115 35 165 38 220 58M128 76C152 104 176 128 215 148" stroke="#6b5344" stroke-width="2.2" stroke-linecap="round" opacity=".45" />
      <circle cx="92" cy="48" r="14" fill="#FADCE6" opacity=".85" />
      <circle cx="155" cy="46" r="16" fill="#F6B8CC" opacity=".8" />
      <circle cx="208" cy="116" r="15" fill="#FADCE6" opacity=".85" />
      <circle cx="222" cy="58" r="12" fill="#F4A5BE" opacity=".75" />
      <ellipse cx="132" cy="64" rx="14" ry="6" transform="rotate(-24 132 64)" fill="#B9D458" opacity=".65" />
      <ellipse cx="182" cy="96" rx="15" ry="6" transform="rotate(18 182 96)" fill="#6D9612" opacity=".45" />
    </svg>

    <!-- 右上樱花枝 -->
    <svg v-else-if="kind === 'sprig-tr'" viewBox="0 0 320 180" fill="none">
      <path d="M-10 35C55 42 125 68 205 118M75 52C115 35 165 38 220 58" stroke="#6b5344" stroke-width="2.2" stroke-linecap="round" opacity=".4" />
      <circle cx="105" cy="48" r="15" fill="#FADCE6" opacity=".85" />
      <circle cx="175" cy="52" r="13" fill="#F6B8CC" opacity=".78" />
      <circle cx="202" cy="114" r="14" fill="#FADCE6" opacity=".82" />
      <ellipse cx="145" cy="72" rx="14" ry="6" transform="rotate(-20 145 72)" fill="#B9D458" opacity=".65" />
    </svg>

    <!-- 左侧悬挂藤蔓 -->
    <svg v-else-if="kind === 'side-l'" viewBox="0 0 160 420" fill="none">
      <path d="M-8 10C38 65 62 145 44 235C28 315 58 370 95 410" stroke="#7a9930" stroke-width="2" stroke-linecap="round" opacity=".5" />
      <path d="M36 96C68 88 95 102 116 124M48 188C82 178 108 192 128 214M40 278C72 270 98 286 118 308" stroke="#8aa844" stroke-width="1.6" stroke-linecap="round" opacity=".48" />
      <ellipse cx="64" cy="88" rx="14" ry="6" transform="rotate(-18 64 88)" fill="#b9d458" opacity=".68" />
      <ellipse cx="88" cy="112" rx="13" ry="5.5" transform="rotate(24 88 112)" fill="#6d9612" opacity=".48" />
      <ellipse cx="76" cy="178" rx="14" ry="6" transform="rotate(-15 76 178)" fill="#b9d458" opacity=".68" />
      <ellipse cx="96" cy="204" rx="13" ry="5.5" transform="rotate(22 96 204)" fill="#6d9612" opacity=".48" />
      <ellipse cx="68" cy="272" rx="14" ry="6" transform="rotate(-20 68 272)" fill="#b9d458" opacity=".65" />
      <circle cx="116" cy="124" r="11" fill="#FADCE6" opacity=".88" />
      <circle cx="116" cy="124" r="4" fill="#d96b8a" opacity=".65" />
      <circle cx="128" cy="214" r="12" fill="#f5b8cb" opacity=".82" />
      <circle cx="118" cy="308" r="10" fill="#FADCE6" opacity=".86" />
      <circle cx="95" cy="410" r="8" fill="#f29cb5" opacity=".78" />
    </svg>

    <!-- 右侧悬挂藤蔓 -->
    <svg v-else-if="kind === 'side-r'" viewBox="0 0 160 420" fill="none">
      <path d="M-8 10C38 65 62 145 44 235C28 315 58 370 95 410" stroke="#7a9930" stroke-width="2" stroke-linecap="round" opacity=".5" />
      <path d="M36 96C68 88 95 102 116 124M48 188C82 178 108 192 128 214M40 278C72 270 98 286 118 308" stroke="#8aa844" stroke-width="1.6" stroke-linecap="round" opacity=".48" />
      <ellipse cx="64" cy="88" rx="14" ry="6" transform="rotate(-18 64 88)" fill="#b9d458" opacity=".68" />
      <ellipse cx="88" cy="112" rx="13" ry="5.5" transform="rotate(24 88 112)" fill="#6d9612" opacity=".48" />
      <ellipse cx="76" cy="178" rx="14" ry="6" transform="rotate(-15 76 178)" fill="#b9d458" opacity=".68" />
      <circle cx="116" cy="124" r="11" fill="#f5b8cb" opacity=".85" />
      <circle cx="128" cy="214" r="12" fill="#FADCE6" opacity=".88" />
      <circle cx="118" cy="308" r="10" fill="#f29cb5" opacity=".8" />
    </svg>

    <!-- 面板右下花簇 -->
    <svg v-else-if="kind === 'panel-flora'" viewBox="0 0 180 140" fill="none">
      <path d="M140 140Q128 92 96 58M152 140Q154 88 134 44M164 140Q172 102 166 68" stroke="#6d9612" stroke-width="2" stroke-linecap="round" opacity=".52" />
      <ellipse cx="112" cy="82" rx="13" ry="5" transform="rotate(-32 112 82)" fill="#b9d458" opacity=".7" />
      <ellipse cx="146" cy="78" rx="12" ry="4.8" transform="rotate(24 146 78)" fill="#6d9612" opacity=".48" />
      <circle cx="96" cy="58" r="10" fill="#FADCE6" opacity=".9" />
      <circle cx="96" cy="58" r="3.5" fill="#d96b8a" opacity=".7" />
      <circle cx="134" cy="44" r="11.5" fill="#f5b8cb" opacity=".85" />
      <circle cx="134" cy="44" r="3.8" fill="#fff" opacity=".9" />
      <circle cx="166" cy="68" r="8.5" fill="#FADCE6" opacity=".85" />
    </svg>
  </span>
</template>

<style>
/* 装饰包裹层：inline-flex；无固定尺寸的 svg 撑满包裹层，带 width 属性（如 logo）保持自身尺寸 */
.fp-decor { display: inline-flex; align-items: flex-start; justify-content: flex-start; line-height: 0; }
.fp-decor > svg { width: 100%; height: auto; display: block; }
.fp-decor > svg[width] { width: auto; }
</style>
