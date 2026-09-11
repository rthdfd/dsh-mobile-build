// 已移除：上游这个手写的 @Component 既没有被任何代码引用，又和 Hilt 的
// @ApplicationContext 绑定冲突（Dagger/MissingBinding）。依赖注入统一由
// @HiltAndroidApp + @InstallIn(SingletonComponent::class) 的模块负责。
//
// 保留空文件是为了让 overlay 覆盖掉上游那份有问题的实现（overlay 只能覆盖，不能删除）。
package com.deepseek.dshmobile.di
