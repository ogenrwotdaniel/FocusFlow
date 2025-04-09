package com.focusflow.data.remote

import android.net.Uri
import com.focusflow.social.UserProfile
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import java.time.LocalDateTime

/**
 * Unit tests for FirebaseRepository
 * Tests Firebase Integration without Firebase Storage
 */
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class FirebaseRepositoryTest {

    @Mock
    private lateinit var mockAuth: FirebaseAuth
    
    @Mock
    private lateinit var mockFirestore: FirebaseFirestore
    
    @Mock
    private lateinit var mockDatabase: FirebaseDatabase
    
    @Mock
    private lateinit var mockMessaging: FirebaseMessaging
    
    @Mock
    private lateinit var mockUser: FirebaseUser
    
    @Mock
    private lateinit var mockCollectionRef: CollectionReference
    
    @Mock
    private lateinit var mockDocRef: DocumentReference
    
    @Mock
    private lateinit var mockDocSnapshot: DocumentSnapshot
    
    @Mock
    private lateinit var mockDatabaseRef: DatabaseReference
    
    @Mock
    private lateinit var mockUri: Uri
    
    private lateinit var repository: FirebaseRepository
    
    @Before
    fun setup() {
        // Set up mocks for Firestore
        `when`(mockFirestore.collection(anyString())).thenReturn(mockCollectionRef)
        `when`(mockCollectionRef.document(anyString())).thenReturn(mockDocRef)
        
        // Set up mocks for Auth
        `when`(mockAuth.currentUser).thenReturn(mockUser)
        `when`(mockUser.uid).thenReturn("test-user-id")
        `when`(mockUser.displayName).thenReturn("Test User")
        
        // Set up mocks for Database
        `when`(mockDatabase.getReference(anyString())).thenReturn(mockDatabaseRef)
        `when`(mockDatabaseRef.child(anyString())).thenReturn(mockDatabaseRef)
        
        // Create repository
        repository = FirebaseRepository()
        
        // Use reflection to set mocked dependencies
        val authField = FirebaseRepository::class.java.getDeclaredField("auth")
        authField.isAccessible = true
        authField.set(repository, mockAuth)
        
        val firestoreField = FirebaseRepository::class.java.getDeclaredField("firestore")
        firestoreField.isAccessible = true
        firestoreField.set(repository, mockFirestore)
        
        val databaseField = FirebaseRepository::class.java.getDeclaredField("database")
        databaseField.isAccessible = true
        databaseField.set(repository, mockDatabase)
        
        val messagingField = FirebaseRepository::class.java.getDeclaredField("messaging")
        messagingField.isAccessible = true
        messagingField.set(repository, mockMessaging)
    }
    
    @Test
    fun `getCurrentUser returns current user`() {
        // When
        val result = repository.getCurrentUser()
        
        // Then
        assert(result != null)
        assert(result?.uid == "test-user-id")
    }
    
    @Test
    fun `uploadProfileImage returns generated avatar URL`() = runTest {
        // When
        val result = repository.uploadProfileImage()
        
        // Then
        assert(result.startsWith("https://ui-avatars.com/api/"))
        assert(result.contains("name=Test+User"))
    }
    
    @Test
    fun `uploadProfileImage returns default avatar when no user is authenticated`() = runTest {
        // Given
        `when`(mockAuth.currentUser).thenReturn(null)
        
        // When
        val result = repository.uploadProfileImage()
        
        // Then
        assert(result.startsWith("https://ui-avatars.com/api/"))
    }
    
    @Test
    fun `getUserProfile returns null when user not found`() = runTest {
        // Given
        val mockTask: Task<DocumentSnapshot> = mock(Task::class.java) as Task<DocumentSnapshot>
        `when`(mockDocRef.get()).thenReturn(mockTask)
        `when`(mockTask.isComplete).thenReturn(true)
        `when`(mockTask.isSuccessful).thenReturn(true)
        `when`(mockTask.result).thenReturn(mockDocSnapshot)
        `when`(mockDocSnapshot.exists()).thenReturn(false)
        
        // When
        val result = repository.getUserProfile("non-existent-user")
        
        // Then
        assert(result == null)
    }
    
    @Test
    fun `updateUserProfile updates user profile without image upload`() = runTest {
        // Given
        val profile = UserProfile(
            userId = "test-user-id",
            displayName = "Updated User",
            email = "test@example.com",
            joinDate = LocalDateTime.now()
        )
        
        val mockTask: Task<Void> = mock(Task::class.java) as Task<Void>
        `when`(mockDocRef.set(any())).thenReturn(mockTask)
        `when`(mockTask.isComplete).thenReturn(true)
        `when`(mockTask.isSuccessful).thenReturn(true)
        
        // When
        val result = repository.updateUserProfile(profile)
        
        // Then
        verify(mockDocRef).set(any())
        assert(result)
    }
}
