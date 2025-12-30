package net.ljga.projects.apps.bttk.testdi
//
//import dagger.Binds
//import dagger.Module
//import dagger.hilt.components.SingletonComponent
//import dagger.hilt.testing.TestInstallIn
//import net.ljga.projects.apps.bttk.domain.repository.DataFrameRepository
//import net.ljga.projects.apps.bttk.data.DataModule
//import net.ljga.projects.apps.bttk.data.FakeDataFrameRepository
//
//@Module
//@TestInstallIn(
//    components = [SingletonComponent::class],
//    replaces = [DataModule::class]
//)
//interface FakeDataModule {
//
//    @Binds
//    abstract fun bindRepository(
//        fakeRepository: FakeDataFrameRepository
//    ): DataFrameRepository
//}
