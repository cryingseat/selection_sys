// 滚动浮现指令：v-reveal
// 元素进入视口时添加 .revealed，配合 CSS .reveal / .revealed 动画与 --d 延迟变量使用
let observer = null

function getObserver() {
  if (!observer) {
    observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) {
            entry.target.classList.add('revealed')
            observer.unobserve(entry.target)
          }
        }
      },
      { threshold: 0.08, rootMargin: '0px 0px -40px 0px' }
    )
  }
  return observer
}

export default {
  mounted(el) {
    if (typeof IntersectionObserver === 'undefined') return
    el.classList.add('reveal')
    getObserver().observe(el)
  },
  unmounted(el) {
    observer?.unobserve(el)
  },
}
