<template>
  <div class="stream-test">
    <h2>流式响应测试</h2>
    
    <div class="test-section">
      <h3>原始流式数据模拟</h3>
      <textarea v-model="rawStreamData" rows="10" cols="80" placeholder="输入模拟的流式数据..."></textarea>
      <button @click="simulateStreamProcessing">模拟流式处理</button>
    </div>

    <div class="test-section" v-if="processedData">
      <h3>处理后的数据</h3>
      <pre>{{ processedData }}</pre>
    </div>

    <div class="test-section" v-if="markdownResult">
      <h3>Markdown渲染结果</h3>
      <MarkdownRenderer :content="markdownResult" />
    </div>

    <div class="test-section">
      <h3>预设测试用例</h3>
      <button @click="loadTestCase1">加载测试用例1（中文空格问题）</button>
      <button @click="loadTestCase3">加载测试用例3（中文空格问题）</button>
      <button @click="loadTestCase4">加载测试用例4（实际LLM空格问题）</button>
      <button @click="loadTestCase2">加载测试用例2（正常代码块）</button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import MarkdownRenderer from './MarkdownRenderer.vue'

const rawStreamData = ref('')
const processedData = ref('')
const markdownResult = ref('')

// 模拟前端store中的流式处理逻辑
const simulateStreamProcessing = () => {
  if (!rawStreamData.value) return

  // 模拟实际的流式处理逻辑
  const lines = rawStreamData.value.split('\n')
  let fullResponse = ''

  for (const line of lines) {
    if (line.trim()) {
      let cleanLine = line.trim()
      if (cleanLine.startsWith('data: ')) {
        cleanLine = cleanLine.substring(6)
      } else if (cleanLine.startsWith('data:')) {
        cleanLine = cleanLine.substring(5)
      }
      if (cleanLine && cleanLine !== ':') {
        // 智能空格处理：清理中文字符之间的多余空格，但保留代码块和英文单词间的必要空格
        if (!cleanLine.startsWith('```') && !cleanLine.includes('    ') && !cleanLine.match(/^\s*#/)) {
          // 对于非代码行的中文内容，清理多余空格
          cleanLine = cleanLine.replace(/([\u4e00-\u9fa5])\s+([\u4e00-\u9fa5])/g, '$1$2')
        }
        
        // 直接添加处理后的内容，不额外添加空格或换行符
        fullResponse += cleanLine
      }
  }

  processedData.value = fullResponse
  markdownResult.value = fullResponse
}

// 加载用户反馈的问题示例（模拟真实LLM流式响应，包含中文内容的多余空格问题）
const loadTestCase1 = () => {
  rawStreamData.value = `data: 我来为你创建一个展示Python类(class)用法的脚本。这个例子会实现一个学生管理系统，包含类的各种核心特性：
data: 
data: \`\`\`python
# -*- coding: utf-8 -*-
"""
Python类(Class)用法示例 - 学生管理系统
展示以下特性：
1. 类的定义和初始化
2. 属性和方法
3. 继承
4. 封装
5. 多态
6. 静态方法和类方法
7. 属性装饰器
"""

class Person:
    """人类基类 - 展示封装和继承"""
    
    # 类变量
    species = "人类"
    
    def __init__(self, name, age):
        """初始化方法"""
        self.name = name      # 实例变量
        self._age = age       # 受保护属性（约定）
        self.__id = None      # 私有属性（名称修饰）
    
    def get_id(self):
        """私有属性的访问方法"""
        return self.__id
    
    def set_id(self, id):
        """私有属性的设置方法"""
        self.__id = id
    
    def introduce(self):
        """实例方法"""
        return f"我叫{self.name}，今年{self._age}岁"
    
    @property
    def age(self):
        """属性装饰器 - 将方法转换为属性"""
        return self._age
    
    @age.setter
    def age(self, value):
        """属性的setter方法"""
        if value < 0:
            raise ValueError("年龄不能为负数")
        self._age = value

data: \`\`\`
data: 以上是完整的Python类示例代码。`
  simulateStreamProcessing()
}

// 模拟中文内容的多余空格问题
const loadTestCase3 = () => {
  rawStreamData.value = `data: Python 基 础 入门 教程 Python 是 一种 简单 易 学
data: 的 编 程 语 言 具 有 丰 富 的 数 据 类 型
data: 和 强 大 的 标 准 库`
  simulateStreamProcessing()
}

// 模拟实际LLM响应中的空格问题
const loadTestCase4 = () => {
  rawStreamData.value = `data: 我 来 为 你 创 建 一 个 Python 脚 本
data: 这 个 脚 本 会 示 范 类 的 用 法
data: 包 括 继 承 、 封 装 、 多 态 等 特 性`
  simulateStreamProcessing()
}

// 加载正常的代码块
const loadTestCase2 = () => {
  rawStreamData.value = `data: 我来为你创建一个展示Python类(class)用法的脚本。这个例子会实现一个学生管理系统，包含类的各种核心特性：
data: 
data: \`\`\`python
# -*- coding: utf-8 -*-
"""
Python类(Class)用法示例 - 学生管理系统
展示以下特性：
1. 类的定义和初始化
2. 属性和方法
3. 继承
4. 封装
5. 多态
6. 静态方法和类方法
7. 属性装饰器
"""

class Person:
    """人类基类 - 展示封装和继承"""
    
    # 类变量
    species = "人类"
    
    def __init__(self, name, age):
        """初始化方法"""
        self.name = name      # 实例变量
        self._age = age       # 受保护属性（约定）
        self.__id = None      # 私有属性（名称修饰）
    
    def get_id(self):
        """私有属性的访问方法"""
        return self.__id
    
    def set_id(self, id):
        """私有属性的设置方法"""
        self.__id = id
    
    def introduce(self):
        """实例方法"""
        return f"我叫{self.name}，今年{self._age}岁"
    
    @property
    def age(self):
        """属性装饰器 - 将方法转换为属性"""
        return self._age
    
    @age.setter
    def age(self, value):
        """属性的setter方法"""
        if value < 0:
            raise ValueError("年龄不能为负数")
        self._age = value

data: \`\`\`
data: 以上是完整的Python类示例代码。`
  simulateStreamProcessing()
}
</script>

<style scoped>
.stream-test {
  padding: 20px;
  max-width: 1000px;
  margin: 0 auto;
}

.test-section {
  margin: 20px 0;
  padding: 15px;
  border: 1px solid #ddd;
  border-radius: 8px;
  background-color: #f9f9f9;
}

.test-section h3 {
  margin-top: 0;
  color: #333;
}

textarea {
  width: 100%;
  padding: 10px;
  border: 1px solid #ccc;
  border-radius: 4px;
  font-family: monospace;
  resize: vertical;
}

button {
  margin: 10px 10px 10px 0;
  padding: 8px 16px;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

button:hover {
  background-color: #0056b3;
}

pre {
  background-color: #f4f4f4;
  padding: 10px;
  border-radius: 4px;
  overflow-x: auto;
  border: 1px solid #ddd;
}
</style>