const PERMISSIONS = {
  microphone: {
    name: 'microphone',
    android: 'android.permission.RECORD_AUDIO',
    description: 'Voice input और speech recognition के लिए microphone access'
  },

  camera: {
    name: 'camera',
    android: 'android.permission.CAMERA',
    description: 'Camera features के लिए camera access'
  },

  notifications: {
    name: 'notifications',
    android: 'android.permission.POST_NOTIFICATIONS',
    description: 'RAJ AI notifications के लिए notification access'
  },

  media: {
    name: 'media',
    android: 'android.permission.READ_MEDIA_IMAGES',
    description: 'Images/media access के लिए Android media permission'
  }
};

function getPermission(name) {
  return PERMISSIONS[name] || null;
}

function listPermissions() {
  return Object.values(PERMISSIONS);
}

module.exports = {
  PERMISSIONS,
  getPermission,
  listPermissions
};
