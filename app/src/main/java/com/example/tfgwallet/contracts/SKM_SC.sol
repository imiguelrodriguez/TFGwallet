// SPDX-License-Identifier: GPL-3.0-or-later

pragma solidity >=0.4.16 <0.9.0;

/** 
 * @title Secure Key Management Smart Contract
 * @dev The Secure Key Management SC allows the user access to the cryptographic keys stored in the IPFS, but also to exchange some information between the plug-in installed in a web browser and the smartphone during the set-up of the system.
 */

contract SKM_SC {

    // Identities of the actors
    address private smartphoneID;

    // address[] private whiteList;
    // mapping(address => uint256) private refsList;

    // Struct to store all plug-ins info.   
    struct DeviceInfo {
        bool exists;
        string IPFSref;
    }
    mapping(address => DeviceInfo) private whiteList;

    bytes private temp;

    /** Constructor
     * @dev Create a new Secure Key Management Smart Contract.
     */
    constructor() {
        smartphoneID = msg.sender;
    }

    /** 
     * @dev Adds a new public key to the whiteList list
     * @param deviceID public key of the new device
     */    
    function addDevice(address deviceID) external onlySmartphone {
        require(!whiteList[deviceID].exists, "This plug-in has already been configured.");
        whiteList[deviceID].exists = true;
    }

    /** 
     * @dev Removes an existing public key from the whiteList list
     * @param deviceID public key of an existing device
     */  
    function removeDevice(address deviceID) external onlySmartphone {
        require(whiteList[deviceID].exists, "This plug-in is not configured.");
        whiteList[deviceID].exists = false;
    }

    /** 
     * @dev Modifies the reference to the IPFS in the mapping position of the device that has used the method
     * @param IPFSref new IPFS reference
     */  
    function storeRef(string memory IPFSref) external {
        require(whiteList[msg.sender].exists, "This plug-in is not configured.");
        whiteList[msg.sender].IPFSref = IPFSref;
    }

    /**
     * @dev Returns IPFS reference 
     */
    function getRef() external view returns (string memory) {
        require(whiteList[msg.sender].exists, "This plug-in is not configured.");
        return whiteList[msg.sender].IPFSref;
    }

    /** 
     * @dev Modifies the reference to the IPFS in the mapping position of the device that has used the method
     * @param IPFSref new IPFS reference
     */  
    function storeRef(address deviceID, string memory IPFSref) external onlySmartphone {
        require(whiteList[deviceID].exists, "This plug-in is not configured.");
        whiteList[deviceID].IPFSref = IPFSref;
    }

    /**
     * @dev Returns IPFS reference 
     */
    function getRef(address deviceID) external onlySmartphone view returns (string memory) {
        require(whiteList[deviceID].exists, "This plug-in is not configured.");
        return whiteList[deviceID].IPFSref;
    }

    /** 
     * @dev Modifies the data contained in the temp field.
     * @param newTemp new temporal value
     */  
    function modTemp(bytes calldata newTemp) external onlySmartphone {
        temp = newTemp;
    }

    /**
     * @dev Returns temporal value 
     */
    function getTemp() external view returns (bytes memory) {
        return temp;
    }

    /**
     * @dev Returns the smartphone's public key
     */
    function getSmartphoneID() external view returns (address) {
        return smartphoneID;
    }

    modifier onlySmartphone() {
        require(msg.sender == smartphoneID, "Only the smartphone is allowed to do this action.");
        _;
    }
}
