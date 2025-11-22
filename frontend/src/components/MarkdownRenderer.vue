<template>
  <div class="markdown-content" v-html="renderedContent" ref="contentRef"></div>
</template>

<script setup>
import { computed, watch, nextTick, ref } from 'vue'
import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'

const props = defineProps({
  content: {
    type: String,
    default: ''
  }
})

const contentRef = ref(null)

// 创建自定义渲染器
const renderer = new marked.Renderer()

// 重写代码块渲染方法
renderer.code = function(code, lang) {
  // 确保code是字符串，并保留原始格式（包括换行符）
  const codeStr = String(code || '')
  
  const validLang = lang && hljs.getLanguage(lang) ? lang : 'plaintext'
  
  // 保留原始代码的换行符和格式
  const originalCode = codeStr.replace(/\r\n/g, '\n').replace(/\r/g, '\n')
  
  let highlightedCode
  try {
    // 使用highlight.js高亮代码
    const result = hljs.highlight(originalCode, { language: validLang })
    highlightedCode = result.value
  } catch (err) {
    console.warn('Highlight.js error:', err)
    try {
      const result = hljs.highlightAuto(originalCode)
      highlightedCode = result.value
    } catch (autoErr) {
      console.warn('Highlight.js auto highlight error:', autoErr)
      // 如果高亮失败，返回HTML转义后的原始代码
      highlightedCode = originalCode.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    }
  }
  
  return `
    <div class="code-block-wrapper">
      <div class="code-block-header">
        <span class="code-language">${validLang}</span>
        <button class="copy-button" data-code="${encodeURIComponent(originalCode)}">复制</button>
      </div>
      <pre><code class="hljs language-${validLang}">${highlightedCode}</code></pre>
    </div>
  `
}

// 重写表格渲染方法
renderer.table = function(header, body) {
  return `
    <table class="markdown-table">
      <thead>
        ${header}
      </thead>
      <tbody>
        ${body}
      </tbody>
    </table>
  `
}

// 重写表格行渲染方法
renderer.tablerow = function(content) {
  return `<tr>${content}</tr>`
}

// 重写表格单元格渲染方法
renderer.tablecell = function(content, flags) {
  const tag = flags.header ? 'th' : 'td'
  const align = flags.align ? ` style="text-align: ${flags.align}"` : ''
  return `<${tag}${align}>${content}</${tag}>`
}

// 配置marked选项 - 移除highlight函数，使用自定义renderer
marked.setOptions({
  renderer: renderer,
  breaks: false, // 不自动转换换行符，保持原始格式
  gfm: true, // GitHub Flavored Markdown
  pedantic: false,
  sanitize: false,
  smartLists: true,
  smartypants: false,
  headerIds: true,
  mangle: false,
  langPrefix: 'hljs language-' // 添加语言前缀
})

// 渲染markdown内容的计算属性
const renderedContent = computed(() => {
  if (!props.content) return ''
  
  try {
    // 确保内容转换为字符串，防止[object Object]问题
    const contentStr = String(props.content)
    return marked.parse(contentStr)
  } catch (error) {
    console.error('Markdown渲染错误:', error)
    return `<div class="markdown-error">Markdown渲染错误: ${error.message}</div>`
  }
})

// 处理复制按钮点击事件
const handleCopyClick = async (button) => {
  const code = decodeURIComponent(button.dataset.code)
  try {
    await navigator.clipboard.writeText(code)
    button.classList.add('success')
    button.textContent = '已复制'
    setTimeout(() => {
      button.classList.remove('success')
      button.textContent = '复制'
    }, 2000)
  } catch (err) {
    console.error('复制失败:', err)
    button.classList.add('error')
    button.textContent = '复制失败'
    setTimeout(() => {
      button.classList.remove('error')
      button.textContent = '复制'
    }, 3000)
  }
}

// 监听内容变化，绑定复制事件
watch(() => props.content, () => {
  nextTick(() => {
    if (contentRef.value) {
      const copyButtons = contentRef.value.querySelectorAll('.copy-button')
      copyButtons.forEach(button => {
        // 移除旧的事件监听器
        button.removeEventListener('click', handleCopyClick)
        // 添加新的事件监听器
        button.addEventListener('click', () => handleCopyClick(button))
      })
    }
  })
}, { immediate: true })
</script>

<style scoped>
.markdown-content {
  line-height: 1.6;
  color: #333;
}

/* 代码块样式 */
.markdown-content :deep(.code-block-wrapper) {
  border: 1px solid #e1e4e8;
  border-radius: 6px;
  margin: 16px 0;
  background-color: #f6f8fa;
  overflow: hidden;
}

.markdown-content :deep(.code-block-header) {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  background-color: #f1f3f4;
  border-bottom: 1px solid #e1e4e8;
  font-size: 12px;
  font-weight: 600;
}

.markdown-content :deep(.code-language) {
  color: #586069;
  text-transform: uppercase;
  font-size: 11px;
}

.markdown-content :deep(.copy-button) {
  background: #ffffff;
  border: 1px solid #d1d5da;
  border-radius: 3px;
  padding: 4px 8px;
  font-size: 11px;
  color: #586069;
  cursor: pointer;
}

.markdown-content :deep(.copy-button:hover) {
  background: #f3f4f6;
  border-color: #c6cbd1;
}

.markdown-content :deep(pre) {
  margin: 0;
  padding: 16px;
  background-color: #f6f8fa;
  border-radius: 0 0 6px 6px;
  overflow-x: auto;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.45;
}

.markdown-content :deep(pre code) {
  background: none;
  padding: 0;
  border-radius: 0;
  font-family: inherit;
  font-size: inherit;
  color: #24292e;
  display: block;
}

/* 内联代码样式 */
.markdown-content :deep(code) {
  background-color: #f3f4f6;
  padding: 2px 4px;
  border-radius: 3px;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 0.9em;
}

/* 表格样式 */
.markdown-content :deep(.markdown-table) {
  border-collapse: collapse;
  width: 100%;
  margin: 16px 0;
  border: 1px solid #e1e4e8;
}

.markdown-content :deep(.markdown-table th),
.markdown-content :deep(.markdown-table td) {
  border: 1px solid #e1e4e8;
  padding: 8px 12px;
  text-align: left;
}

.markdown-content :deep(.markdown-table th) {
  background-color: #f6f8fa;
  font-weight: 600;
}

/* 其他元素样式 */
.markdown-content :deep(blockquote) {
  border-left: 4px solid #dfe2e5;
  padding-left: 16px;
  margin: 16px 0;
  color: #6a737d;
}

.markdown-content :deep(h1),
.markdown-content :deep(h2),
.markdown-content :deep(h3),
.markdown-content :deep(h4),
.markdown-content :deep(h5),
.markdown-content :deep(h6) {
  margin-top: 24px;
  margin-bottom: 16px;
  font-weight: 600;
  line-height: 1.25;
}

.markdown-content :deep(h1) {
  font-size: 2em;
  border-bottom: 1px solid #eaecef;
  padding-bottom: 0.3em;
}

.markdown-content :deep(h2) {
  font-size: 1.5em;
  border-bottom: 1px solid #eaecef;
  padding-bottom: 0.3em;
}

.markdown-content :deep(h3) {
  font-size: 1.25em;
}

.markdown-content :deep(p) {
  margin-bottom: 16px;
}

.markdown-content :deep(ul),
.markdown-content :deep(ol) {
  padding-left: 2em;
  margin-bottom: 16px;
}

.markdown-content :deep(li) {
  margin-bottom: 4px;
}

.markdown-content :deep(img) {
  max-width: 100%;
  height: auto;
  margin: 16px 0;
}

/* 复制按钮状态 */
.markdown-content :deep(.copy-button.success) {
  background: #28a745;
  border-color: #28a745;
  color: white;
}

.markdown-content :deep(.copy-button.error) {
  background: #dc3545;
  border-color: #dc3545;
  color: white;
}
</style>