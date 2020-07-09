server_address="http://localhost:8055"

function sendRequest(serverURL, jsonData, onResponse) {
  $.ajax({
        method: "POST",
        url: serverURL,
        data: JSON.stringify(jsonData),
        success: function(dataRec){
            onResponse(dataRec);
            console.log(dataRec);
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
        map_inps = { };
        for (i=0;i<inps.length; i++) {
            map_inps[inps[i].name] = inps[i].value;
        }
        //map_inps["confirm_password"]="none";
        sendRequest(server_address+"/regForm", map_inps, onRegisterConfirmed);
   });
}

function onRegisterConfirmed(jsonData) {
  console.log(jsonData);
}

function setSendBillFile() {
  $("form#uploadFile").submit(function (e) {
      e.preventDefault();
      var map_inps = { };
      var formDat = new FormData();
      formDat.append("billFile",$("#pdfFile").prop("files")[0]);
      formDat.append("cycle_billing", $("#isCycle")[0].checked);
      formDat.append("bill_type", $(".selectpicker")[0].value);
      $.ajax({
            data: formDat,
            method: "POST",
            url: server_address+"/uploadFile",
            processData: false,
            contentType: false,
            success: function(dataRec){
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
    pay_map = { };
    for (i=0;i<rows.length;i++) {
      row=rows[i];
      tds = $("td", row);
      pay_map[tds[0].textContent] = $("input", tds[1])[0].value;
    }
    supplier_map = {"pay_map": pay_map};
    supplier_map["supplier"] = $("#supplierType")[0].value;
    supplier_map["billOwner"] = $("#billOwner")[0].value;
    sendRequest(server_address+"/regForm", supplier_map, console.log);
}

function setOnLoginRequest() {
  $("form#loginForm").submit(function (e) {
      e.preventDefault();
      inps = $("input", $("#loginForm")[0]);
      map_inps = { };
      map_inps[inps[0].title] = inps[0].value;
      map_inps[inps[1].title] = sjcl.codec.hex.fromBits(sjcl.hash.sha256.hash(inps[1].value));
      sendRequest(server_address+"/regForm", map_inps, console.log);
 });
}

function setOnAddRoomateRequest() {
  $("form#addRoomate").submit(function (e) {
      e.preventDefault();
      inps = $("input", $("#addRoomate")[0]);
      map_inps = { };
      for (i=0;i<inps.length;i++){
        map_inps[inps[i].name]=inps[i].value;
      }
      sendRequest(server_address+"/regForm", map_inps, console.log);
 });
}

function setOnSendPayment() {
  $("form#payRoomateForm").submit(function (e) {
      e.preventDefault();
      inps = $(".payFormProp");
      map_inps = { };
      for (i=0;i<inps.length; i++) {
          map_inps[inps[i].name] = inps[i].value;
      }
      //map_inps["confirm_password"]="none";
      sendRequest(server_address+"/regForm", map_inps, onRegisterConfirmed);
      sendRequest("10.100.102.7:8055"+"/regForm", map_inps, onRegisterConfirmed);
 });
}

function getMessages() {
  messages = { }; // Server call
}

function getRoomates() {
    server_url = server_address + "/getInfo";
    jsonInfo = {"requestType": "roomates", "token": "aaaa"};
    onResp = dat => {
        partPerRoomate = calculatePartPerRoomate(dat["roomates"].length);
        dat["roomates"].map(roomate=>{
            addOptionToSelectPicker($("#billOwner"), roomate);
            addRoomateToSplit($("#roomatesSplit"), roomate, partPerRoomate[dat["roomates"].indexOf(roomate)]);
        });
    };
    sendRequest(server_url, jsonInfo, onResp);
}

function getBillSummary() {
    server_url = server_address + "/getInfo";
    jsonInfo = {"requestType": "billSummary", "token": "aaaa"};
    onResp = dat => {
        dat["billSummary"].map(rowJson=>{
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
    inputPart.value = partPercentage +"%";
    partTd.append(inputPart);
    newRow.append(nameTd);
    newRow.append(partTd);
    splitPartNode.append(newRow);
}

function calculatePartPerRoomate(numOfRoomates) {
    partPerRoomate = (new Array(numOfRoomates)).fill(Math.floor(100/numOfRoomates));
    partPerRoomate[0] += 100 - partPerRoomate.reduce((x, y)=>x+y);
    return partPerRoomate;
}

function addRowToBillSummary(summaryRowsNode, rowJson) {
    newRow = document.createElement("tr");
    tds =  { };
    for (info in rowJson) {
        newTd = document.createElement("td");
        newTd.textContent = rowJson[info];
        tds[info.toLowerCase()] = newTd;
    }
    newRow.append(tds["supplier"]);
    newRow.append(tds["payer"]);
    newRow.append(tds["address"]);
    newRow.append(tds["bill_number"]);
    newRow.append(tds["account_number"]);
    newRow.append(tds["amount"]);
    newRow.append(tds["paying_date"]);
    summaryRowsNode.append(newRow);
}

function getGeneralSummary() {
    server_url = server_address + "/getInfo";
    jsonInfo = {"requestType": "generalSummary", "token": "aaaa"};
    onResp = dat => {
        dat["generalSummary"].map(rowJson=>{
            addRowToGeneralSummary($("#generalSummaryRows"), rowJson);
        });
    };
    sendRequest(server_url, jsonInfo, onResp);
}

function addRowToGeneralSummary(generalSummaryRowsNode, rowJson) {
    newRow = document.createElement("tr");
    tds =  { };
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
        action_ref.href="#";
        newRow.style.backgroundColor="greenyellow"
    }
    else if (rowJson["status"].toLowerCase() === "need to pay") {
        action_ref.textContent = "Pay";
        action_ref.href="#";
        newRow.style.backgroundColor="#cc0000"
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