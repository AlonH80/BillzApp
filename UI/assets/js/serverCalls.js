server_address="http://localhost:8055"

function sendRequest(serverURL, jsonData, onResponse) {
  $.ajax({
        method: "POST",
        url: serverURL,
        data: JSON.stringify(jsonData),
        success: function(dataRec){
            //onRespone(dataRec);
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
      sendRequest("10.100.102.7:8055"+"/regForm", map_inps, onRegisterConfirmed
 });
}
