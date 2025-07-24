package com.example.digi_diary.di

import android.content.Context
import com.example.digi_diary.data.AppDatabase
import com.example.digi_diary.data.local.RoomNoteLocalDataSource
import com.example.digi_diary.data.remote.FirestoreNoteRemoteDataSource
import com.example.digi_diary.data.remote.NoteRemoteDataSource
import com.example.digi_diary.data.repository.NoteRepository
import com.example.digi_diary.data.repository.NoteRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

/**
 * Dagger module that provides data-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }
    
    @Provides
    @Singleton
    fun provideLocalDataSource(
        database: AppDatabase,
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): RoomNoteLocalDataSource {
        return RoomNoteLocalDataSource(database, context, ioDispatcher)
    }
    
    @Provides
    @Singleton
    fun provideRemoteDataSource(): NoteRemoteDataSource {
        return FirestoreNoteRemoteDataSource()
    }
    
    @Provides
    @Singleton
    fun provideNoteRepository(
        localDataSource: RoomNoteLocalDataSource,
        remoteDataSource: NoteRemoteDataSource,
        @ApplicationScope applicationScope: CoroutineScope,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): NoteRepository {
        return NoteRepositoryImpl(
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource,
            applicationScope = applicationScope,
            ioDispatcher = ioDispatcher
        )
    }
    
    @Provides
    @Singleton // Changed from @ApplicationScope to match the component scope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(Dispatchers.Default)
    
    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
