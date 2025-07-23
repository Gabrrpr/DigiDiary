const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

exports.changePassword = functions.https.onCall(async (data, context) => {
    console.log('changePassword called with data:', JSON.stringify(data, null, 2));
    
    // Ensure user is authenticated
    if (!context.auth) {
        const error = 'User not authenticated';
        console.error(error);
        throw new functions.https.HttpsError(
            'unauthenticated',
            'You must be logged in to change your password.'
        );
    }

    const { currentPassword, newPassword } = data;
    console.log(`Processing password change for user: ${context.auth.uid}`);
    
    if (!currentPassword || !newPassword) {
        const error = 'Missing required fields';
        console.error(error);
        throw new functions.https.HttpsError(
            'invalid-argument',
            'Current and new password are required.'
        );
    }

    const userId = context.auth.uid;
    const email = context.auth.token.email || null;
    
    if (!email) {
        const error = 'User email not found in token';
        console.error(error);
        throw new functions.https.HttpsError(
            'invalid-argument',
            'User email not found.'
        );
    }

    try {
        console.log(`Attempting to update password for user: ${email} (${userId})`);
        
        // 1. Get the user
        const user = await admin.auth().getUser(userId);
        console.log('User found:', user.email);
        
        // 2. Update the password using Admin SDK
        console.log('Updating password...');
        await admin.auth().updateUser(userId, {
            password: newPassword
        });
        console.log('Password updated successfully');

        // 3. Revoke all refresh tokens to sign out all sessions
        console.log('Revoking refresh tokens...');
        await admin.auth().revokeRefreshTokens(userId);
        console.log('Refresh tokens revoked');

        return { 
            success: true,
            message: 'Password updated successfully',
            timestamp: new Date().toISOString()
        };
    } catch (error) {
        console.error('Password change error:', {
            code: error.code,
            message: error.message,
            stack: error.stack
        });
        
        if (error.code === 'auth/wrong-password') {
            throw new functions.https.HttpsError(
                'permission-denied',
                'Incorrect current password.'
            );
        }
        
        throw new functions.https.HttpsError(
            'internal',
            `Failed to update password: ${error.message || 'Unknown error'}`
        );
    }
});
