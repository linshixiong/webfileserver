// Put your custom code here

	var isDownloadClicked;
	var mkdirOK;
	var downloadFileName;
	
		
	function cloeseToast(){
		$("#toast").popup( "close" );
	}
	
	function pageInit (event){
		console.log("pageInit");
		$( "#toast" ).on( "popupafteropen", function( event, ui ) {	
			window.setTimeout(cloeseToast,800);		
		} );
		
		$( "#downloadDialog" ).on( "popupafterclose", function( event, ui ) {	
			console.log("downloadDialog popupafterclose");
			$( "#downloadDialog" ).popup( "close");
		});
		$( "#uploadDialog" ).on( "popupafterclose", function( event, ui ) {	
			xhr.abort();
			var target=document.getElementById("uploadProgress");
			target.style.display="none"; 
		} );
		
		$( "#popupAudio" ).on( "popupafterclose", function( event, ui ) {	
			var audio = document.getElementById("audio_player");
			audio.pause();
		} );
		
		$( "#popupVideo" ).on( "popupafterclose", function( event, ui ) {	
			var video = document.getElementById("video_player");
			video.pause();
		} );
		
		
		
		$("#filelist > li").click(function ()  {  
			console.log("filelistClick");
			if($(this).attr("isDirectory")=="true")
			{
				return;
			}		
			 var length= $("#filelist > li").length;
			 for(var i=0;i<length;i++)
			 {
				var o= $("#filelist > li").get(i);
				if($(o).attr("isDirectory")=="true")
				{
					continue;
				}
				
				$(o).buttonMarkup({ icon: "file" });
				$(o).find("a").removeClass("checked"); 	
				
			 }
			$(this).buttonMarkup({ icon: "file-checked" });
			$(this).addClass("checked"); 
			$(this).find("a").addClass("checked"); 
		});
		
		$("#file").change(function () {
			
            var file = document.getElementById('file').files[0];
			if (file) {
				var fileSize = getFileSize(file.size);

				$("#uploadDialog").popup("open");
			}
			document.getElementById('fileName').innerHTML = '名称: ' + file.name;
			document.getElementById('fileSize').innerHTML = '大小: ' + fileSize;

        });
	
	}
	
		function pageShow(event,ui){
			console.log("page show");
			$(ui.prevPage).remove();
			
		}
	
	
	function getSelectedFile(){
	
		var length= $("#filelist > li").length;
		var selected; 
		for(var i=0;i<length;i++)
		{
			var o= $("#filelist > li").get(i);
			if($(o).attr("isDirectory")=="true")
			{
				continue;
			}
			var data_icon= $(o).attr("data-icon");
			console.log("downloadFile data_icon="+data_icon);
			if(data_icon=="file-checked"){
				selected=o;
				break;
			}		
		}
		return selected;
	}
	
  	function downloadFile()
	{		
		var selected=getSelectedFile();
		if(selected==undefined){
			console.log("请选择需要下载的文件.");
			$("#toast_msg").html("请选择需要下载的文件.");
			$("#toast" ).popup( "option", "transition", "pop" );
			$("#toast" ).popup( "open", "" );
			
		}
		else{
			console.log("设置下载链接...");
			var fileName=$(selected).find("img").attr("alt");
			var fileSize=$(selected).attr("fileSize");
			downloadFileName=fileName;
			$("#downloadFileName").html("文件名："+fileName);
			$("#downloadFileSize").html("文件大小："+getFileSize(fileSize));
			$("#download_file_url").attr("href",fileName);
			$("#download_file_url").attr("download",fileName);
			$( "#downloadDialog" ).popup( "option", "transition", "pop" );
			$( "#downloadDialog" ).popup( "open", "" );

		}
			
	}
	
	
	function startDownloadFile()
	{
		//console.log("startDownloadFile...");
		
		window.setTimeout(closeDownloadDialog,100);	
	}
	
	function closeDownloadDialog()
	{	
		$("#downloadDialog" ).popup( "close");
	}
	
	function getFileSize(length)
    {
    	
    	if(length<1024){
    		return length+ "B";
    	}
    	length=length/1024;
    	if(length<1024){
    		return  toDecimal(length)+ "KB";
    	}
    	length=length/1024;
    	if(length<1024){
    		return  toDecimal(length)+ "MB";
    	}
    	length=length/1024;
    
    	return  toDecimal(length)+ "GB";
    	
    }
	
	function toDecimal(x) {    
        var f = parseFloat(x);    
        if (isNaN(f)) {    
            return;    
        }    
        f = Math.round(x*100)/100;    
        return f;    
    }    
	
	function selectFile()
	{
		 $("#file").click();  
	}

	function showDownloadDialog()
	{
		$("#downloadDialog").popup("open");
	}
	
	function  mkdir(){
		var dirName=$('#dir_name').val();
		var xhr = new XMLHttpRequest();
		xhr.addEventListener("load", mkdirComplete, false);
		xhr.addEventListener("error", mkdirComplete, false);
		xhr.open("POST", ".");
		console.log("mkdir :"+dirName);
		xhr.send("mkdir="+dirName);
		$("#mkdirDialog").popup("close");
	}	
		
	 function mkdirComplete(evt) {
		$("#filelist").html(evt.target.responseText);
		$("#filelist").listview('refresh'); 
		//$("#filelist > li").click(filelistClick);
		//location.reload() ;
	 }

	var xhr;
	function uploadFile() {
        var fd = new FormData();
        fd.append("file", document.getElementById('file').files[0]);
        xhr = new XMLHttpRequest();
        xhr.upload.addEventListener("progress", uploadProgress, false);
		xhr.upload.addEventListener("loadstart", uploadStart, false);
        xhr.addEventListener("load", uploadComplete, false);
        xhr.addEventListener("error", uploadComplete, false);
        xhr.addEventListener("abort", uploadCanceled, false);
        xhr.open("POST", ".");
        xhr.send(fd);
      }
	  function uploadStart(evt){
		  console.log("uploadStart");
		   var target=document.getElementById("uploadProgress"); 
           target.style.display="block";
            
	  }
	  
      function uploadProgress(evt) {
        if (evt.lengthComputable) {
          var percentComplete = Math.round(evt.loaded * 100 / evt.total);
          document.getElementById('progressNumber').innerHTML = "正在上传，完成"+ percentComplete.toString() + '%';
        }
        else {
          document.getElementById('progressNumber').innerHTML = '正在上传...';
        }
      }
      function uploadComplete(evt) {
		$("#uploadDialog").popup("close");

		$("#filelist").html(evt.target.responseText);
		$("#filelist").listview('refresh'); 
		//$("#filelist > li").click(filelistClick);
      }
      function uploadFailed(evt) {
        //alert("There was an error attempting to upload the file.");
      }
      function uploadCanceled(evt) {
        
      }

	
	function openFile(){
		//$("#action").attr("href","#NONE");
		var selected=getSelectedFile();
		if(selected==undefined){
			$("#toast_msg").html("请选择需要打开的文件.");
			$("#toast" ).popup( "option", "transition", "pop" );
			$("#toast" ).popup( "open", "" );			
		}
		
		else{
			var fileName=$(selected).find("img").attr("alt");
			var dialog;
			if(fileName.lastIndexOf(".")>0){
				var extName=fileName.substring(fileName.lastIndexOf(".")+1,fileName.length).toLowerCase();
				console.log("file extension name is "+extName);
				
				//打开图片预览
				if(extName=="jpg"||extName=="jpeg"||extName=="bmp"||extName=="png"||extName=="gif"){
				
					console.log("view image file "+fileName);
								
					$("#picture_view").attr("src",fileName);
					dialog=$("#popupPicture" );
				}
				else if(extName=="mp3"||extName=="ogg"||extName=="wav"){
					console.log("play audio "+fileName);
					var audio = $("#audio_player");
					audio.attr("src",fileName);
					//audio.load();
					//audio.play();
					dialog=$("#popupAudio" );
				}else if (extName=="mp4"){
					console.log("play video "+fileName);
					var video = $("#video_player");
					video.attr("src",fileName);
					//video.load();
					//video.play();
					dialog=$("#popupVideo" );
				}else
				{
					$("#toast_msg").html("此文件类型无法直接打开，请下载此文件!");
					dialog=$("#toast");
				}
				
			}else{
				$("#toast_msg").html("此文件类型无法直接打开，请下载此文件!");
				dialog=$("#toast");
			}
			dialog.popup( "option", "transition", "pop" );
			dialog.popup( "open", "" );
		
		}
	}	
	