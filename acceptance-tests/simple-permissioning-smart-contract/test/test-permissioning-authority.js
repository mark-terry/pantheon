const TestPermissioning = artifacts.require('PermissioningWithAuthority.sol');
var proxy;

var newAdmin = "f17f52151EbEF6C7334FAD080c5704D77216b732";

contract('Permissioning WITH AUTHORITY ', () => {
  describe('Function: Permissioning + Authority', () => {
    it('add an admin and check if they are authorized', async () => {
      proxy = await TestPermissioning.new();
      let result = await proxy.addAdmin(newAdmin);

      // add them a second time, result should be false
      result = await proxy.addAdmin(newAdmin);

      result = await proxy.isAuthorized(newAdmin);
      assert.equal(result, true, 'expected new admin to be authorized');

      result = await proxy.removeAdmin(newAdmin);
      result = await proxy.isAuthorized(newAdmin);
      assert.equal(result, false, 'expected disallowed since admin was removed');
    });

  });
});
