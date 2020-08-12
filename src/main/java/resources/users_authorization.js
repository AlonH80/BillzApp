authServer = "https://localhost:8003";

function onLoginClick() {
    var userId = $("#userId")[0].value;
    var password = $("#password")[0].value;
    console.log(userId);
    console.log(password);
    var jsonData = {"userId": userId, "password": password, "type": "login"};
    sendRequest(authServer, jsonData, onRes);
}

function onChangeClick() {
    var userId = $("#userId")[0].value;
    var password = $("#password")[0].value;
    var newPassword = $("#newPassword")[0].value;
    console.log(userId);
    console.log(password);
    console.log(newPassword);
    var jsonData = {"userId": userId, "password": password, "newPassword": newPassword, "type": "change"};
    sendRequest(authServer, jsonData, onRes);
}

function onSetClick() {
    var userId = $("#userId")[0].value;
    var password = $("#password")[0].value;
    console.log(userId);
    console.log(password);
    var jsonData = {"userId": userId, "password": password, "type": "set"};
    sendRequest(authServer, jsonData, onRes);
}
function onRes(jsonData) {
    console.log(jsonData);
    if ("error" in jsonData){
        $( "body" ).append("<div>Error: " + jsonData.error + "</div>")
    }
    else{
        $( "body" ).append("<div> Status: " + jsonData.status  +"</div>")
    }
}
// function sendRequest(jsonData) {
//     $.ajax({
//         type: "POST",
//         url: authServer,
//         data: JSON.stringify(jsonData),
//         success: function(jsonData ){
//             console.log(jsonData);
//             if ("error" in jsonData){
//                 $( "body" ).append("<div>Error: " + jsonData.error + "</div>")
//             }
//             else{
//                 $( "body" ).append("<div> Status: " + jsonData.status  +"</div>")
//             }
//         },
//         error: function (e) {
//             console.log("status code: " + e.status.toString());
//         }
//     });
// }

function sendRequest(serverURL, jsonData, onResponse) {
    $.ajax({
        type: "POST",
        url: serverURL,
        data: JSON.stringify(jsonData),
        success: function(dataRec){
            onResponse(dataRec);
        },
        error: function (e) {
            console.log("status code: " + e.status.toString());
        }
    });
}