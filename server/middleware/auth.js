const { auth } = require('../firebase');

module.exports = async function(req, res, next) {
  const token = req.header('Authorization')?.split(' ')[1];

  if (!token) {
    return res.status(401).json({ success: false, message: 'No token, authorization denied' });
  }

  try {
    const decodedToken = await auth.verifyIdToken(token);
    req.user = decodedToken;   // now req.user.uid, req.user.email etc. all work
    next();
  } catch (err) {
    console.warn('Firebase token validation failed:', err.code || err.message);
    res.status(401).json({ success: false, message: 'Token is not valid' });
  }
};
