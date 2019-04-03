const TestPermissioning = artifacts.require('PermissioningWithAuthority.sol');
var proxy;

var node1High = "0x9bd359fdc3a2ed5df436c3d8914b1532740128929892092b7fcb320c1b62f375";
var node1Low = "0x892092b7fcb320c1b62f3759bd359fdc3a2ed5df436c3d8914b1532740128929";
var node1Host = "0x9bd359fd";
var node1Port = 30303;

var node2High = "0x892092b7fcb320c1b62f3759bd359fdc3a2ed5df436c3d8914b1532740128929";
var node2Low = "0x892092b7fcb320c1b62f3759bd359fdc3a2ed5df436c3d8914b1532740128929";
var node2Host = "0x9bd359fd";
var node2Port = 30304;

contract('Permissioning Ipv4', () => {
  describe('Function: permissioning Ipv4', () => {
    it('Should add a node to the whitelist and then permit that node', async () => {
      proxy = await TestPermissioning.new();
      await proxy.addEnodeIpv4(node1High, node1Low, node1Host, node1Port);
      
      let resultEnodes = await proxy.getAllEnodeHighsIpv4();
      console.log(resultEnodes);
      assert.equal(resultEnodes[0].toLowerCase(), node1High.toLowerCase(), 'expected first enode');
      assert.equal(resultEnodes.length, 1, 'expected 1 only');

      // adding same node again should not change the list
      await proxy.addEnodeIpv4(node1High, node1Low, node1Host, node1Port);
      resultEnodes = await proxy.getAllEnodeHighsIpv4();
      console.log(resultEnodes);
      assert.equal(resultEnodes[0].toLowerCase(), node1High.toLowerCase(), 'expected first enode');
      assert.equal(resultEnodes.length, 1, 'expected 1 only - second add should have failed');
 
      // add another
      await proxy.addEnodeIpv4(node2High, node2Low, node2Host, node2Port);

      resultEnodes = await proxy.getAllEnodeHighsIpv4();
      console.log(resultEnodes);
      assert.equal(resultEnodes[0].toLowerCase(), node1High.toLowerCase(), 'expected first enode');
      assert.equal(resultEnodes[1].toLowerCase(), node2High.toLowerCase(), 'expected second enode');
      assert.equal(resultEnodes.length, 2, 'expected 2 only');
    });

    it('test get enode string', async () => {
      let str = await proxy.enodeBytes(node1High, node1Low, node1Host, node1Port);
      console.log(str);
    });
  });
});
