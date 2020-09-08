function sendRequest(serverURL, jsonData, onResponse) {
    jsonData["apartmentId"] = sessionStorage.getItem("apartmentId") || "0";
    jsonData["userId"] = sessionStorage.getItem("user_name");
    $.ajax({
        method: "POST",
        url: serverURL,
        data: JSON.stringify(jsonData),
        success: function (dataRec) {
            onResponse(dataRec);
            console.log(dataRec);
        },
        error: function (e) {
            console.log("status code: " + e.status.toString());
        }
    });
}

function sendGetRequest(serverURL, onResponse) {
    $.ajax({
        method: "GET",
        url: serverURL,
        success: function (dataRec) {
            onResponse(dataRec);
            //console.log(dataRec);
        },
        error: function (e) {
            console.log("status code: " + e.status.toString());
        }
    });
}


function setRegisterForm() {
    $("form#signupForm").submit(function (e) {
        e.preventDefault();
        inps = $(".form-control");
        map_inps = {};
        for (i = 0; i < inps.length; i++) {
            map_inps[inps[i].name] = inps[i].value;
        }
        map_inps["type"] = "set";
        map_inps["userId"] = map_inps["user_name"];
        map_inps["apartmentId"] = "0";
        //map_inps["confirm_password"]="none";
        sendRequest(server_address, map_inps, onRegisterConfirmed);
    });
}

function setLoginForm() {
    $("form#loginForm").submit(function (e) {
        e.preventDefault();
        inps = $(".form-control");
        map_inps = {};
        for (i = 0; i < inps.length; i++) {
            map_inps[inps[i].name] = inps[i].value;
        }
        map_inps["type"] = "login";
        map_inps["userId"] = map_inps["user_name"];
        sessionStorage.setItem("user_name", map_inps["userId"]);
        //map_inps["confirm_password"]="none";
        sendRequest(server_address, map_inps, onLoginConfirmed);
    });
}

function setChangePassForm() {
    $("form#changePassForm").submit(function (e) {
        e.preventDefault();
        inps = $(".form-control");
        map_inps = {};
        for (i = 0; i < inps.length; i++) {
            map_inps[inps[i].name] = inps[i].value;
        }
        map_inps["type"] = "change_password";
        map_inps["token"] = sessionStorage.getItem("token");
        map_inps["user_name"] = sessionStorage.getItem("user_name");
        //map_inps["confirm_password"]="none";
        sendRequest(server_address, map_inps, onLoginConfirmed);
    });
}

function onRegisterConfirmed(jsonData) {
    console.log(jsonData);
    if (jsonData["status"] === "success") {
        sessionStorage.setItem("user_name", jsonData["userId"]);
        sessionStorage.setItem("apartmentId", jsonData["apartmentId"]);
        setToken("jimbo");
        //window.location.href = server_address + "/index.html";
        onPageApproved(server_address + "/index.html");
    } else {
        unsuccessNode = $("#unsuccess-reg")[0];
        unsuccessNode.textContent = "Error: " + jsonData["error"];
    }
}

function onLoginConfirmed(jsonData) {
    console.log(jsonData);
    if (jsonData["status"] === "success") {
        setToken("jimbo");
        sessionStorage.setItem("apartmentId", jsonData["apartmentId"]);
        //window.location.href = server_address + "/index.html";
        onPageApproved(server_address + "/index.html");
    } else {
        sessionStorage.removeItem("user_name");
        unsuccessNode = $("#unsuccess-log")[0];
        unsuccessNode.textContent = "Error: " + jsonData["error"];
    }
}

function requestForPage(pageURL) {
    jsonInfo = {
        "page": pageURL,
        "token": sessionStorage.getItem("user_token"),
        "userId": sessionStorage.getItem("user_name")
    };
    sendRequest(server_address, jsonInfo, data => {
        onPageApproved(data["page"]);
    });
}

function onPageApproved(pageURL) {
    window.location.href = server_address + "/index.html";
}

function setToken(token) {
    sessionStorage.removeItem("user_token");
    sessionStorage.setItem("user_token", token);
}

function setHelloLabel() {
    helloNode = $("#welcome-header")[0];
    if (sessionStorage.getItem("user_name") != null) {
        helloNode.textContent = "Welcome " + sessionStorage.getItem("user_name") + "!";
    }
}

function setHeyLabel() {
    helloNode = $("#hey-header")[0];
    if (sessionStorage.getItem("user_name") != null) {
        helloNode.textContent = "Hey " + sessionStorage.getItem("user_name") + "!";
    }
}

function setSendBillFile() {
    $("form#uploadFile").submit(function (e) {
        e.preventDefault();
        var map_inps = {};
        var formDat = new FormData();
        formDat.append("billFile", $("#pdfFile").prop("files")[0]);
        formDat.append("cycle_billing", $("#isCycle")[0].checked);
        formDat.append("bill_type", $(".selectpicker")[0].value);
        $.ajax({
            data: formDat,
            method: "POST",
            url: server_address + "/uploadFile",
            processData: false,
            contentType: false,
            success: function (dataRec) {
                console.log(dataRec);
            },
            error: function (e) {
                console.log("status code: " + e.status.toString());
            }
        });
    });
}

function getAndPutParticipants() {
    rows = $("tr").slice(1);
    pay_map = {};
    for (i = 0; i < rows.length; i++) {
        row = rows[i];
        tds = $("td", row);
        pay_map[tds[0].textContent] = $("input", tds[1])[0].value;
    }
    supplier_map = {"partsMap": pay_map};
    supplier_map["supplier"] = $("#supplierType")[0].value.split(' ').join('_');
    ;
    supplier_map["billOwner"] = $("#billOwner")[0].value;
    supplier_map["type"] = "addSupplier";
    supplier_map["apartmentId"] = sessionStorage.getItem("apartmentId");
    supplier_map["userId"] = sessionStorage.getItem("user_name");
    sendRequest(server_address, supplier_map, console.log);
}

// function setOnLoginRequest() {
//   $("form#loginForm").submit(function (e) {
//       e.preventDefault();
//       inps = $("input", $("#loginForm")[0]);
//       map_inps = { };
//       map_inps[inps[0].title] = inps[0].value;
//       map_inps[inps[1].title] = sjcl.codec.hex.fromBits(sjcl.hash.sha256.hash(inps[1].value));
//       sendRequest(server_address+"/regForm", map_inps, console.log);
//  });
// }

function setOnAddRoomateRequest() {
    $("form#addRoomate").submit(function (e) {
        e.preventDefault();
        inps = $("input", $("#addRoomate")[0]);
        map_inps = {};
        for (i = 0; i < inps.length; i++) {
            map_inps[inps[i].name] = inps[i].value;
        }
        sendRequest(server_address + "/regForm", map_inps, console.log);
    });
}

function setOnSendPayment() {
    $("form#payRoomateForm").submit(function (e) {
        e.preventDefault();
        inps = $(".payFormProp");
        map_inps = {};
        for (i = 0; i < inps.length; i++) {
            map_inps[inps[i].name] = inps[i].value;
        }
        //map_inps["confirm_password"]="none";
        sendRequest(server_address, map_inps, onRegisterConfirmed);
        sendRequest(server_address, map_inps, onRegisterConfirmed);
    });
}

function getMessages() {
    server_url = server_address;
    jsonInfo = {"type": "messages", "token": "aaaa", "userId": sessionStorage.getItem("user_name"), "apartmentId": sessionStorage.getItem("apartmentId")};
    onResp = dat => {
        dat.map(msgJson => {
            createMsgRow($("#msgsContainer"), msgJson);
        });
    };
    sendRequest(server_url, jsonInfo, onResp);
}

function getRoomates() {
    server_url = server_address;
    jsonInfo = {
        "type": "roommates",
        "token": "aaaa",
        "apartmentId": sessionStorage.getItem("apartmentId"),
        "userId": sessionStorage.getItem("user_name")
    };
    onResp = dat => {
        partPerRoomate = calculatePartPerRoomate(dat.length);
        dat.map(roomate => {
            addOptionToSelectPicker($("#billOwner"), roomate);
            addRoomateToSplit($("#roomatesSplit"), roomate, partPerRoomate[dat.indexOf(roomate)]);
        });
    };
    sendRequest(server_url, jsonInfo, onResp);
}

function getBillSummary() {
    server_url = server_address;
    jsonInfo = {"type": "getBills", "token": "aaaa", "apartmentId": sessionStorage.getItem("apartmentId")};
    onResp = dat => {
        dat.map(rowJson => {
            addRowToBillSummary($("#summaryRows"), rowJson);
        });
    };
    sendRequest(server_url, jsonInfo, onResp);
}

function addOptionToSelectPicker(selectPickerNode, optionContent) {
    optionNode = document.createElement("option");
    optionNode.textContent = optionContent;
    selectPickerNode.append(optionNode);
    selectPickerNode.selectpicker("refresh");
}

function addRoomateToSplit(splitPartNode, roomateName, partPercentage) {
    newRow = document.createElement("tr");
    nameTd = document.createElement("td");
    partTd = document.createElement("td");
    inputPart = document.createElement("input");
    nameTd.textContent = roomateName;
    inputPart.type = "text";
    inputPart.value = partPercentage + "%";
    partTd.append(inputPart);
    newRow.append(nameTd);
    newRow.append(partTd);
    splitPartNode.append(newRow);
}

function calculatePartPerRoomate(numOfRoomates) {
    partPerRoomate = (new Array(numOfRoomates)).fill(Math.floor(100 / numOfRoomates));
    partPerRoomate[0] += 100 - partPerRoomate.reduce((x, y) => x + y);
    return partPerRoomate;
}

function addRowToBillSummary(summaryRowsNode, rowJson) {
    newRow = document.createElement("tr");
    tds = {};
    for (info in rowJson) {
        newTd = document.createElement("td");
        newTd.textContent = rowJson[info];
        tds[info] = newTd;
    }
    newRow.append(tds["billType"]);
    newRow.append(tds["owner"]);
    // newRow.append(tds["address"]);
    // newRow.append(tds["bill_number"]);
    // newRow.append(tds["account_number"]);
    newRow.append(tds["amount"]);
    newRow.append(tds["dDay"]);
    newRow.append(tds["status"]);
    summaryRowsNode.append(newRow);
}

function getGeneralSummary() {
    server_url = server_address + "/getInfo";
    jsonInfo = {"type": "generalSummary", "token": "aaaa"};
    onResp = dat => {
        dat["generalSummary"].map(rowJson => {
            addRowToGeneralSummary($("#generalSummaryRows"), rowJson);
        });
    };
    sendRequest(server_url, jsonInfo, onResp);
}

function addRowToGeneralSummary(generalSummaryRowsNode, rowJson) {
    newRow = document.createElement("tr");
    tds = {};
    for (info in rowJson) {
        newTd = document.createElement("td");
        newTd.textContent = rowJson[info];
        tds[info.toLowerCase()] = newTd;
    }
    action_ref = document.createElement("a");
    action_ref.classList.add("button");
    action_ref.classList.add("special");
    if (rowJson["status"].toLowerCase() === "paid") {
        action_ref.textContent = "Details";
        action_ref.href = "#";
        newRow.style.backgroundColor = "greenyellow"
    } else if (rowJson["status"].toLowerCase() === "need to pay") {
        action_ref.textContent = "Pay";
        action_ref.href = "#";
        newRow.style.backgroundColor = "#cc0000"
    }
    tds["action"] = document.createElement("td");
    tds["action"].append(action_ref);
    newRow.append(tds["bill"]);
    newRow.append(tds["date"]);
    newRow.append(tds["status"]);
    newRow.append(tds["amount"]);
    newRow.append(tds["action"]);
    generalSummaryRowsNode.append(newRow);
}

function getBalance() {
    server_url = server_address + "/getInfo";
    jsonInfo = {"type": "balance", "token": "aaaa"};
    onResp = dat => {
        max_balance = dat["balance"].map(r => Math.abs(parseInt(r.balance))).reduce((x, y) => x > y ? x : y);
        power = 2;
        while (max_balance >= Math.pow(10, power)) {
            power++;
        }
        max_balance = Math.pow(10, power);
        dat["balance"].map(rowJson => {
            addRoomateToBalance($("#balanceRow"), rowJson, max_balance);
        });
    };
    sendRequest(server_url, jsonInfo, onResp);
}

function addRoomateToBalance(balanceRowNode, rowJson, max_balance) {
    intBalance = parseInt(rowJson["balance"]);
    intAbsBalance = Math.abs(intBalance);
    newTh = document.createElement("th");
    header0 = document.createElement("h3");
    header1 = document.createElement("h3");
    barNode = createBarNode(intBalance, max_balance);
    if (parseInt(rowJson["balance"]) >= 0) {
        header0.textContent = "You owe " + rowJson["roomate"] + ":";
    } else {
        header0.textContent = rowJson["roomate"] + " owe you:";
    }
    header1.textContent = intAbsBalance.toString() + " $";
    newTh.append(header0);
    newTh.append(barNode);
    newTh.append(header1);
    balanceRowNode.append(newTh);
}

function createBarNode(balance, maxBalance) {
    divMainBar = document.createElement("div");
    divMainBar.classList.add("col");
    divMainBar.classList.add("chart");
    divMainBar.classList.add("small-font-size");

    divSecBar = document.createElement("div");
    translateYPerc = parseInt((Math.abs(balance) / maxBalance) * 100);
    divSecBar.classList.add("bar");
    divSecBar.classList.add("bar-" + translateYPerc.toString());
    if (balance > 0) {
        divSecBar.classList.add("yellow");
    } else if (balance < 0) {
        divSecBar.classList.add("red");
    } else {
        divSecBar.classList.add("lime");
    }

    divFace0 = document.createElement("div");
    divFace0.classList.add("face");
    divFace0.classList.add("side-0");

    divFace1 = document.createElement("div");
    divFace1.classList.add("face");
    divFace1.classList.add("side-1");

    divGrowingBar1 = document.createElement("div");
    divGrowingBar1.classList.add("growing-bar");
    divGrowingBar2 = document.createElement("div");
    divGrowingBar2.classList.add("growing-bar");
    divFace0.append(divGrowingBar1);
    divFace1.append(divGrowingBar2);

    divFaceTop = document.createElement("div");
    divFaceTop.classList.add("face");
    divFaceTop.classList.add("top");
    divFaceBottom = document.createElement("div");
    divFaceBottom.classList.add("face");
    divFaceBottom.classList.add("floor");

    divSecBar.append(divFace0);
    divSecBar.append(divFace1);
    divSecBar.append(divFaceTop);
    divSecBar.append(divFaceBottom);

    divMainBar.append(divSecBar);

    return divMainBar;
}

// function getSvgNode(msgType) {
//     type_json = {
//         "debt": {
//             "bi_class": "bi-exclamation-octagon-fill",
//             "d": "M11.46.146A.5.5 0 0011.107 0H4.893a.5.5 0 00-.353.146L.146 4.54A.5.5 0 000 4.893v6.214a.5.5 0 00.146.353l4.394 4.394a.5.5 0 00.353.146h6.214a.5.5 0 00.353-.146l4.394-4.394a.5.5 0 00.146-.353V4.893a.5.5 0 00-.146-.353L11.46.146zM8 4a.905.905 0 00-.9.995l.35 3.507a.552.552 0 001.1 0l.35-3.507A.905.905 0 008 4zm.002 6a1 1 0 100 2 1 1 0 000-2z"
//         },
//         "roomate_joined" :{
//             "bi_class": "bi-person-plus-fill",
//             "d": "M1 14s-1 0-1-1 1-4 6-4 6 3 6 4-1 1-1 1H1zm5-6a3 3 0 100-6 3 3 0 000 6zm7.5-3a.5.5 0 01.5.5v2a.5.5 0 01-.5.5h-2a.5.5 0 010-1H13V5.5a.5.5 0 01.5-.5z",
//             "d2": "M13 7.5a.5.5 0 01.5-.5h2a.5.5 0 010 1H14v1.5a.5.5 0 01-1 0v-2z"
//         },
//         "payment_approved" :{
//             "bi_class": "bi-check-circle",
//             "d": "M15.354 2.646a.5.5 0 010 .708l-7 7a.5.5 0 01-.708 0l-3-3a.5.5 0 11.708-.708L8 9.293l6.646-6.647a.5.5 0 01.708 0z",
//             "d2": "M8 2.5A5.5 5.5 0 1013.5 8a.5.5 0 011 0 6.5 6.5 0 11-3.25-5.63.5.5 0 11-.5.865A5.472 5.472 0 008 2.5z"
//         },
//         "pay_reminder" :{
//             "bi_class": "bi-bell-fill",
//             "d": "M8 16a2 2 0 002-2H6a2 2 0 002 2zm.995-14.901a1 1 0 10-1.99 0A5.002 5.002 0 003 6c0 1.098-.5 6-2 7h14c-1.5-1-2-5.902-2-7 0-2.42-1.72-4.44-4.005-4.901z",
//         },
//         "user_joined" :{
//             "bi_class": "bi-house-door-fill",
//             "d": "M6.5 10.995V14.5a.5.5 0 01-.5.5H2a.5.5 0 01-.5-.5v-7a.5.5 0 01.146-.354l6-6a.5.5 0 01.708 0l6 6a.5.5 0 01.146.354v7a.5.5 0 01-.5.5h-4a.5.5 0 01-.5-.5V11c0-.25-.25-.5-.5-.5H7c-.25 0-.5.25-.5.495z",
//             "d2": "M13 2.5V6l-2-2V2.5a.5.5 0 01.5-.5h1a.5.5 0 01.5.5z"
//         }
//     };
//     svgNode = document.createElementNS("http://www.w3.org/2000/svg", "svg");
//     svgNode.classList.add("bi");
//     svgNode.classList.add(type_json[msgType].bi_class);
//     svgNode.classList.add(type_json[msgType].bi_class);
//
//     svgNode.setAttribute("width", "1em"); svgNode.setAttribute("height", "1em");
//     svgNode.setAttribute("viewBox", "0 0 16 16"); svgNode.setAttribute("fill", "currentColor");
//     svgNode.setAttribute("xmlns", "http://www.w3.org/2000/svg");
//
//     path1 = document.createElement("path");
//     path1.setAttribute("fill-rule","evenodd");
//     path1.setAttribute("d", type_json[msgType].d);
//     path1.setAttribute("clip-rule","evenodd");
//     svgNode.append(path1);
//     if ("d2" in type_json[msgType]) {
//         path2 = document.createElement("path");
//         path2.setAttribute("d", type_json[msgType].d2);
//         path2.d=type_json[msgType].d2;
//         path2.setAttribute("clip-rule","evenodd");
//         svgNode.append(path2);
//     }
//     return svgNode;
// }

function createMsgRow(msgsNode, msgJson) {
    msgRow = document.createElement("tr");
    //svgTd = document.createElement("td");
    msgTd = document.createElement("td");
    //svgNode = getSvgNode(msgJson["type"]);
    //svgTd.appendChild(svgNode);
    imgNode = createImgNode(msgJson["type"]);
    msgTd.appendChild(imgNode);
    textNode = document.createElement("span");
    textNode.style.setProperty("margin-left", "10px");
    textNode.innerText = msgJson["message"];
    msgTd.appendChild(textNode);
    //msgRow.append(svgTd);
    msgRow.append(msgTd);
    msgsNode.append(msgRow);
}

function createImgNode(msgType) {
    type_json = {
        "debt": "pay.png",
        "roomate_joined": "add-friend.png",
        "payment_approved": "list.png",
        "pay_reminder": "bell.png",
        "user_joined": "home-run.png"
    };
    imgNode = document.createElement("img");
    imgNode.src = server_address + "/images/" + type_json[msgType];
    imgNode.style.setProperty("width", "20px");
    imgNode.style.setProperty("height", "20px");
    return imgNode;
}

function redirectToPage(page) {

    window.location.href = server_address + "/" + page;
}

function logUserOut() {
    sessionStorage.removeItem("user_token");
    sessionStorage.removeItem("user_name");
    window.location.href = server_address;
    // call to server to erase session
}

function getSuppliers() {
    server_url = server_address;
    jsonInfo = {"type": "getSuppliers", "token": "aaaa", "apartmentId": sessionStorage.getItem("apartmentId")};
    onResp = dat => {
        dat.map(jsonData => {
            createHouseBox(jsonData);
        });
    };
    sendRequest(server_url, jsonInfo, onResp);
}

function createHouseBox(jsonData) {
    let supplier = jsonData["type"];
    let owner = jsonData["ownerId"];
    let roomates = jsonData["roommates"]; // type: json {roomate, part}
    let suppliers_container = $("#suppliers-container")[0];
    let houseboxNode = document.createElement("div");
    let supplier_header = document.createElement("div");
    let supplier_info = document.createElement("div");
    let owner_header = document.createElement("div");
    let owner_info = document.createElement("div");

    supplier_header.style.setProperty("margin-top", "3px");
    supplier_header.style.setProperty("padding", "0px");
    supplier_header_label = document.createElement("label");
    supplier_header_label.style.setProperty("font-weight", "bold");
    supplier_header_label.style.setProperty("font-size", "100%");
    supplier_header_label.textContent = "Supplier";
    supplier_header.appendChild(supplier_header_label);
    houseboxNode.appendChild(supplier_header);

    supplier_info.style.setProperty("margin-top", "3px");
    supplier_info.style.setProperty("padding", "0px");
    supplier_info_label = document.createElement("label");
    supplier_info_label.style.setProperty("font-weight", "bold");
    supplier_info_label.style.setProperty("font-size", "100%");
    supplier_info_label.textContent = supplier;
    supplier_info.appendChild(supplier_info_label);
    houseboxNode.appendChild(supplier_info);

    owner_header.style.setProperty("margin-top", "2px");
    owner_header.style.setProperty("padding", "0px");
    owner_header_label = document.createElement("label");
    owner_header_label.style.setProperty("font-weight", "bold");
    owner_header_label.style.setProperty("font-size", "100%");
    owner_header_label.textContent = "Owner";
    owner_header.appendChild(owner_header_label);
    houseboxNode.appendChild(owner_header);

    owner_info.style.setProperty("margin-top", "2px");
    owner_info.style.setProperty("padding", "0px");
    owner_info_label = document.createElement("label");
    owner_info_label.style.setProperty("font-weight", "bold");
    owner_info_label.style.setProperty("font-size", "100%");
    owner_info_label.textContent = owner;
    owner_info.appendChild(owner_info_label);
    houseboxNode.appendChild(owner_info);

    let billSplitHeader = document.createElement("div");
    billSplitHeader.style.setProperty("color", "black");
    billSplitHeader.style.setProperty("margin-bottom", "0px");
    billSplitHeader.style.setProperty("vertical-align", "center");
    billSplitHeader.style.setProperty("font-weight", "bold");
    billSplitHeader.textContent = "Bill split";
    houseboxNode.appendChild(billSplitHeader);

    let emptySpan = document.createElement("span");
    houseboxNode.appendChild(emptySpan);

    let roomatesBox = document.createElement("ul");
    roomatesBox.classList.add("roomates");
    for (i in roomates) {
        let roomateRow = document.createElement("li");
       // let rooomates_str = roomates[i]["roomate"].padEnd(20) + roomates[i]["part"];
        roomatesBox.appendChild(roomateRow);
    }
    houseboxNode.appendChild(roomatesBox);
    houseboxNode.appendChild(emptySpan);

    suppliers_container.appendChild(houseboxNode);
}

function changeSetting(setting_id) {
    input_nd = $("#" + setting_id)[0];
    input_value = input_nd.value;
    requestJson = {"type": "changeSetting", "setting": setting_id, "value": input_value, "userId": sessionStorage.getItem("user_name")};
    onChangeDone = jsonData => {
        if (jsonData["status"] === "success") {
            redirectToPage("settings.html");
        } else {
            $("#change-err")[0].textContent = jsonData["error"];
        }
    };
    sendRequest(server_address, requestJson, onChangeDone);
}

function getResource(resource_rel_path) {
    on_response = data => {
        return data;
    };
    sendGetRequest(server_address + "/" + resource_rel_path, on_response);
}

function createCreateAptButton() {
    let btnNode = document.createElement("button");
    btnNode.classList.add("apt-action");
    btnNode.textContent = "Create an apartment";
    btnNode.onclick = () => {
        reqMap = {"userId": sessionStorage.getItem("user_name"), "token": "aaaa", "type": "createApartment"};
        sendRequest(server_address, reqMap, console.log);
    };
    //redirectToPage("createApartment.html");
    $("#apt-action-nd")[0].appendChild(btnNode);
}

function createInviteRoomateButton() {
    let btnNode = document.createElement("button");
    btnNode.classList.add("apt-action");
    btnNode.textContent = "Invite User !";
    btnNode.onclick = () => redirectToPage("invite.html");
    $("#apt-action-nd")[0].appendChild(btnNode);
}

function generateUserHome() {
    setHelloLabel();
    let userAptId = sessionStorage.getItem("apartmentId");
    if (userAptId !== "0" && userAptId !== undefined) {
        createInviteRoomateButton();
        getBalance();
    } else {
        createCreateAptButton();
    }
}

function sendManualBilling() {
    inputNd = $("#manualBill")[0];
    inps = $("input", inputNd);
    map_inps = {};
    for (i = 0; i < inps.length; i++) {
        map_inps[inps[i].name] = inps[i].value;
    }
    map_inps["apartmentId"] = sessionStorage.getItem("apartmentId");
    map_inps["userId"] = sessionStorage.getItem("user_name");
    map_inps["billType"] = $(".selectpicker")[0].value;
    map_inps["type"] = "addBill";
    onResp = $("#res_div")[0].textContent = "Check for new bill";
    sendRequest(server_address, map_inps, onResp);
}