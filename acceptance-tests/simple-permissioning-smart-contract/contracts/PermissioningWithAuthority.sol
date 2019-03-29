pragma solidity >=0.4.0 <0.6.0;
// THIS CONTRACT IS FOR TESTING PURPOSES ONLY
// DO NOT USE THIS CONTRACT IN PRODUCTION APPLICATIONS

contract PermissioningWithAuthority {
    // These will be assigned at the construction
    // phase, where `msg.sender` is the account
    // creating this contract.
    address public admin = msg.sender;
    bool readOnlyMode = false;
    struct Admin {
        address adminAddress;
        bool active;
    }
    mapping(address => Admin) admins;
    uint adminCount;
    address[] adminKeys;

    struct EnodeIpv6 {
        bytes32 enodeHigh;
        bytes32 enodeLow;
        bytes16 enodeHost; // Ipv6
        uint16 enodePort;
    }
    struct EnodeIpv4 {
        bytes32 enodeHigh;
        bytes32 enodeLow;
        bytes4 enodeHost; // Ipv4
        uint16 enodePort;
    }
    mapping(bytes => EnodeIpv6) private whitelistIpv6; // should there be a size for the whitelists?
    mapping(bytes => EnodeIpv4) private whitelistIpv4; 

    // keys
    uint countIpv4;
    uint countIpv6;
    bytes[] keysIpv4;
    bytes[] keysIpv6;


    // AUTHORIZATION
    constructor () public {
        // add the deploying contract address as first admin
        Admin memory orig = Admin(msg.sender, true);
        admins[msg.sender] = orig;
        adminKeys.push(msg.sender);
        adminCount = adminCount + 1;
    }

    // AUTHORIZATION: LIST OF ADMINS
    modifier onlyAdmin() 
    {
        require(
            admins[msg.sender].adminAddress != address(0),
         "Sender not authorized."
        ); 
        _;
    }

    function isAuthorized(address source) public view returns (bool) {
        return admins[source].active;
    }

    function addAdmin(address _newAdmin) public onlyAdmin returns (bool) {
        if (admins[_newAdmin].active) {
            return false;
        }
        adminKeys.push(_newAdmin); 
        Admin memory newAdmin = Admin(_newAdmin, true);
        admins[_newAdmin] = newAdmin;
        adminCount = adminCount + 1;
        return true;
    }

    function removeAdmin(address oldAdmin) public onlyAdmin returns (bool) {
        admins[oldAdmin].active = false;
        adminCount = adminCount - 1;
        return true;
    }
    // return list of admins
    function getAllAdmins() public view returns (address[] memory){
    address[] memory ret = new address[](adminCount);
    Admin memory a;
    for (uint i = 0; i < adminKeys.length; i++) {
        a = admins[adminKeys[i]];
        if (a.active) {
            ret[i] = admins[adminKeys[i]].adminAddress;
        }
    }
    return ret;
    }

    // READ ONLY MODE
    function isReadOnly() public view returns (bool) {
        return readOnlyMode;
    }

    function enterReadOnly() public onlyAdmin returns (bool) {
        require(readOnlyMode == false);
        readOnlyMode = true;
        return true;
    }
    function exitReadOnly() public onlyAdmin returns (bool) {
        require(readOnlyMode == true);
        readOnlyMode = false;
        return true;
    }

    // RULES - IS CONNECTION ALLOWED
    function connectionAllowedIpv6(
        bytes32 sourceEnodeHigh, bytes32 sourceEnodeLow, bytes16 sourceEnodeIpv6, uint16 sourceEnodePort, 
        bytes32 destinationEnodeHigh, bytes32 destinationEnodeLow, bytes16 destinationEnodeIpv6, uint16 destinationEnodePort) 
        public view returns (bool) {
        return (enodeAllowedIpv6(sourceEnodeHigh, sourceEnodeLow, sourceEnodeIpv6, sourceEnodePort) && 
        enodeAllowedIpv6(destinationEnodeHigh, destinationEnodeLow, destinationEnodeIpv6, destinationEnodePort));
    }
    function connectionAllowedIpv4(
        bytes32 sourceEnodeHigh, bytes32 sourceEnodeLow, bytes4 sourceEnodeIpv4, uint16 sourceEnodePort, 
        bytes32 destinationEnodeHigh, bytes32 destinationEnodeLow, bytes4 destinationEnodeIpv4, uint16 destinationEnodePort) 
        public view returns (bool){
        return (enodeAllowedIpv4(sourceEnodeHigh, sourceEnodeLow, sourceEnodeIpv4, sourceEnodePort) && 
        enodeAllowedIpv4(destinationEnodeHigh, destinationEnodeLow, destinationEnodeIpv4, destinationEnodePort));
    }

    // RULES - IS ENODE ALLOWED
    function enodeAllowedIpv6(bytes32 sourceEnodeHigh, bytes32 sourceEnodeLow, bytes16 sourceEnodeIpv6, uint16 sourceEnodePort) 
    public view returns (bool){
        bytes memory key = computeKeyIpv6(sourceEnodeHigh, sourceEnodeLow, sourceEnodeIpv6, sourceEnodePort);
        EnodeIpv6 storage whitelistSource = whitelistIpv6[key];
        if (enodeExists(whitelistSource)) {
            return true;
        }
    }
    function enodeAllowedIpv4(bytes32 sourceEnodeHigh, bytes32 sourceEnodeLow, bytes4 sourceEnodeIpv4, uint16 sourceEnodePort) 
    public view returns (bool){
        bytes memory key = computeKeyIpv4(sourceEnodeHigh, sourceEnodeLow, sourceEnodeIpv4, sourceEnodePort);
        EnodeIpv4 storage whitelistSource = whitelistIpv4[key];
        if (enodeExists(whitelistSource)) {
            return true;
        }
    }

    // RULES MODIFIERS - ADD
    function addEnodeIpv6(bytes32 enodeHigh, bytes32 enodeLow, bytes16 enodeIpv6, uint16 enodePort) public onlyAdmin returns (bool) {
        require(readOnlyMode == false);
        EnodeIpv6 memory newEnode = EnodeIpv6(enodeHigh, enodeLow, enodeIpv6, enodePort);
        bytes memory key = computeKeyIpv6(enodeHigh, enodeLow, enodeIpv6, enodePort);
        // return false if already in the list
        if (enodeExists(whitelistIpv6[key])) {
            return false;
        }
        whitelistIpv6[key] = newEnode;
        keysIpv6.push(key);
        countIpv6 = countIpv6 + 1;
        return true;
    }
    function addEnodeIpv4(bytes32 enodeHigh, bytes32 enodeLow, bytes4 enodeIpv4, uint16 enodePort) public onlyAdmin returns (bool) {
        require(readOnlyMode == false);
        EnodeIpv4 memory newEnode = EnodeIpv4(enodeHigh, enodeLow, enodeIpv4, enodePort);
        bytes memory key = computeKeyIpv4(enodeHigh, enodeLow, enodeIpv4, enodePort);
        // return false if already in the list
        if (enodeExists(whitelistIpv4[key])) {
            return false;
        }
        whitelistIpv4[key] = newEnode;
        keysIpv4.push(key);
        countIpv4 = countIpv4 + 1;
        return true;
    }

    // RULES MODIFIERS - REMOVE
    function removeEnodeIpv6(bytes32 enodeHigh, bytes32 enodeLow, bytes16 enodeIpv6, uint16 enodePort) public onlyAdmin returns (bool) {
        require(readOnlyMode == false);
        bytes memory key = computeKeyIpv6(enodeHigh, enodeLow, enodeIpv6, enodePort);
        if (enodeExists(whitelistIpv6[key])) {
            return false;
        }
        countIpv6 = countIpv6 - 1;
        // TODO does this work?
        delete whitelistIpv6[key];
        return true;
    }
    function removeEnodeIpv4(bytes32 enodeHigh, bytes32 enodeLow, bytes4 enodeIpv4, uint16 enodePort) public onlyAdmin returns (bool) {
        require(readOnlyMode == false);
        bytes memory key = computeKeyIpv4(enodeHigh, enodeLow, enodeIpv4, enodePort);
        // TODO is this enough to check?
        if (whitelistIpv4[key].enodeHigh == 0) {
            return false;
        }
        countIpv4 = countIpv4 - 1;
        delete whitelistIpv4[key];
        return true;
    }

    // return list of enodes
    // TODO so far just enodeHigh ie bytes32[]
    function getAllEnodeHighsIpv4() public view returns (bytes32[] memory){
    bytes32[] memory ret = new bytes32[](countIpv4);
    EnodeIpv4 memory a;
    for (uint i = 0; i < keysIpv4.length; i++) {
        a = whitelistIpv4[keysIpv4[i]];
        if (enodeExists(a)) {
            ret[i] = whitelistIpv4[keysIpv4[i]].enodeHigh;
        }
    }
    return ret;
    }

    // RULES - UTILS
    function enodeBytes(EnodeIpv4 memory enode) private pure returns (bytes memory) {
        return enodeBytes(enode.enodeHigh, enode.enodeLow, enode.enodeHost, enode.enodePort);
    }
    function enodeBytes(bytes32 enodeHigh, bytes32 enodeLow, bytes4 enodeHost, uint16 enodePort) public pure returns (bytes memory) {
        // TODO 32 + 32 + 4 + uint16 port
        uint16 enodeLength = 72;
        bytes memory ret = new bytes(enodeLength);
        for (uint i=0; i < 32; i++) {
            ret[i] = enodeHigh[i];
        }
        for (uint i=0; i < 32; i++) {
            ret[32 + i] = enodeLow[i];
        }
        for (uint i=0; i < 4; i++) {
            ret[64 + i] = enodeHost[i];
        }
        // TODO don't need 32 bytes here
        // use last 4 bytes but is that too many?
        bytes32 portBytes = bytes32(uint256(enodePort));
        for (uint i=0; i < 4; i++) {
            ret[68 + i] = portBytes[i+28];
        }
        return ret;
    }

    function enodeExists(EnodeIpv4 memory enode) private pure returns (bool) {
        // TODO do we need to check all fields?
        return enode.enodeHost > 0 && enode.enodeHigh > 0 && enode.enodeLow > 0;
    }
    function enodeExists(EnodeIpv6 memory enode) private pure returns (bool) {
        return enode.enodeHost > 0 && enode.enodeHigh > 0 && enode.enodeLow > 0;
    }

    function computeKeyIpv6(bytes32 enodeHigh, bytes32 enodeLow, bytes16 enodeIpv6, uint16 enodePort) public pure returns (bytes memory) {
        return abi.encode(enodeHigh, enodeLow, enodeIpv6, enodePort);
    }
    function computeKeyIpv4(bytes32 enodeHigh, bytes32 enodeLow, bytes4 enodeIpv4, uint16 enodePort) public pure returns (bytes memory) {
        return abi.encode(enodeHigh, enodeLow, enodeIpv4, enodePort);
    }

    function getKeyCountIpv4() public view returns (uint) {
        return keysIpv4.length;
    }

    function getKeyCountIpv6() public view returns (uint) {
        return keysIpv6.length;
    }
}
