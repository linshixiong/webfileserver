$(document).ready(function(){
		$("div a").bind("click", function(e){
		var action= $(this).attr("action");
		var data= $(this).attr("data");
		doAction(action,data);
	}); 
});


function doAction(action,data){
	var xhr = new XMLHttpRequest();

	xhr.addEventListener("load", sendComplete, false);
	xhr.addEventListener("error", sendComplete, false);
	xhr.open("POST","/");
	xhr.setRequestHeader("Content-Type","application/x-www-form-urlencoded");  
	xhr.send("remote_action="+action+"&data="+data);
}

 function sendComplete(evt) {
	
}