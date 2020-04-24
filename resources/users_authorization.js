authServer = "https://localhost:8003";

function onLoginClick() {
    var userId = $("#userId")[0].value;
    var password = $("#password")[0].value;
    console.log(userId);
    console.log(password);
    var jsonData = {"userId": userId, "password": password, "type": "login"};
    sendRequest(jsonData);
}

function onChangeClick() {
    var userId = $("#userId")[0].value;
    var password = $("#password")[0].value;
    var newPassword = $("#newPassword")[0].value;
    console.log(userId);
    console.log(password);
    console.log(newPassword);
    var jsonData = {"userId": userId, "password": password, "newPassword": newPassword, "type": "change"};
    sendRequest(jsonData);
}

function onSetClick() {
    var userId = $("#userId")[0].value;
    var password = $("#password")[0].value;
    console.log(userId);
    console.log(password);
    var jsonData = {"userId": userId, "password": password, "type": "set"};
    sendRequest(jsonData);
}

function sendRequest(jsonData) {
    $.ajax({
        type: "POST",
        url: authServer,
        data: JSON.stringify(jsonData),
        success: function(jsonData ){
            //jsonData = JSON.parse(data);
            console.log(jsonData);
            if ("error" in jsonData){
                $( "body" ).append("<div>Error: " + jsonData.error + "</div>")
            }
            else{
                $( "body" ).append("<div> Status: " + jsonData.status  +"</div>")
            }
        },
        error: function (e) {
            console.log("status code: " + e.status.toString());
        }
    });
}