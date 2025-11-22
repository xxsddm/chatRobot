# Python类用法示例测试

我来为你创建一个展示Python类(class)用法的脚本。这个例子会实现一个学生管理系统，包含类的各种核心特性：

```python
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
    
    @staticmethod
    def get_species():
        """静态方法"""
        return Person.species
    
    @classmethod
    def create_adult(cls, name):
        """类方法"""
        return cls(name, 18)


class Student(Person):
    """学生类 - 展示继承"""
    
    def __init__(self, name, age, student_id):
        super().__init__(name, age)
        self.student_id = student_id
        self.grades = []
    
    def add_grade(self, grade):
        """添加成绩"""
        self.grades.append(grade)
    
    def get_average_grade(self):
        """计算平均成绩"""
        if not self.grades:
            return 0
        return sum(self.grades) / len(self.grades)
    
    # 方法重写 - 展示多态
    def introduce(self):
        return f"我是学生{self.name}，学号：{self.student_id}"


# 使用示例
if __name__ == "__main__":
    # 创建实例
    person1 = Person("张三", 25)
    student1 = Student("李四", 20, "S001")
    
    # 测试方法
    print(person1.introduce())
    print(student1.introduce())  # 多态体现
    
    # 测试属性装饰器
    print(f"年龄：{person1.age}")
    person1.age = 26
    print(f"更新后的年龄：{person1.age}")
```

## 测试结果

上面的代码块应该正确显示：
- 所有换行符都正确保留
- 代码缩进格式完整
- 语法高亮正常工作
- 复制功能可用