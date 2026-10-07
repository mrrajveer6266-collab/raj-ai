const { listPermissions, getPermission } = require('./permissions');

function listRequiredPermissions() {
  return listPermissions();
}

function getPermissionInfo(name) {
  const permission = getPermission(name);

  if (!permission) {
    throw new Error(`Permission "${name}" उपलब्ध नहीं है।`);
  }

  return permission;
}

module.exports = {
  listRequiredPermissions,
  getPermissionInfo
};
