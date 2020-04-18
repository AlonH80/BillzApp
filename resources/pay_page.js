paymentServer = "https://localhost:8002";

function onPayReqClick() {
    var userIdFrom = $("#userIdFrom")[0].value;
    var userIdTo = $("#userIdTo")[0].value;
    var amount = $("#amount")[0].value;
    console.log(userIdTo);
    console.log(userIdFrom);
    console.log(amount);
    payRequest(userIdFrom, userIdTo, amount);
}

function payRequest(userIdFrom, userIdTo, amount) {
    var jsonData = {"userIdFrom": userIdFrom, "userIdTo": userIdTo, "amount": amount};
    $.ajax({
        type: "POST",
        url: paymentServer,
        data: JSON.stringify(jsonData),
        success: function(data){
            jsonData = JSON.parse(data);
            console.log(jsonData);
            if ("error" in jsonData){
                $( "body" ).append("<div>Error: " + jsonData.error + "</div>")
            }
            else{
                $( "body" ).append("<a href='" + jsonData.approval_url  +"'>" + jsonData.approval_url + "</a>")
            }
        },
        error: function (e) {
            console.log("status code: " + e.status.toString());
        }
    });
}