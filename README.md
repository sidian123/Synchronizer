* 关于字符编码
    
    仅在数据库连接时需要, 其他时候不需要, 即允许结构同步时存在字符编码不同的情况
    
* 仅支持MySQL

    若要支持其他数据库, 需要自己修改代码来适配

* 关于时区

    写死了, 东八区

* 关于使用

    见测试类, 主要是提供数据库连接信息(Database), 然后开始同步(Synchronizer), 最后会输出SQL补丁

* bug
    
    - [x] 未考虑到auto_increment的约束, auto_increment列必须存在索引
    
    - [ ] 直接修改目标, 可能同步不了字段之间的顺序
    
* 未完成
    
    - [ ] scheme不存在时自动创建
    
* 实现

    * `auto_increment`约束与索引之间关系的处理
        
        ```
        新增,修改和添加列时, 需要考虑到auto_increment与索引的约束
        
        添加auto_increment列时
          1. 先添加列声明, 无auto_increment约束
          2. 添加列索引
          3. 修改, 添加auto_increment约束
        
        修改auto_increment列时
          1. 删除auto_increment约束
              先删除auto_increment约束,再删除索引
          2. 添加auto_increment约束
              填添加索引, 再添加auto_increment约束
        
        删除auto_increment列时
          该列的索引不用管了;
          或者, 将删除操作之后, 让对应的索引先删除
        ```