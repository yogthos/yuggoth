$(document).ready(function(){	    
	SyntaxHighlighter.config.tagName = "code";
	SyntaxHighlighter.defaults.gutter = false;   
	SyntaxHighlighter.all();
    if ($("#post-preview").length != 0) renderPreview();
    $($("#selected").val()).toggleClass("selected");
  	 
  	$(".help").click(showHelp); 
  	$(".render-preview").click(renderPreview("#content"));
  	$(".render-comment-preview").click(renderPreview("#comment"));
  	$(".submit").keypress(onEnter);
  	$(".submit-comment").keypress(onEnter);
  	$(".submit-comment").click(submitComment);  	
  	$(".tagoff").click(toggleTag);
  	$(".tagon").click(toggleTag);  	
  	$(".submit").click(function(){$(this).parents("FORM").submit();});  	
    $(".delete").click(function(e){
    	confirm(this.parentNode);
    	// Always return false here since we don't know what jConfirm is going to do
    	return false;
  	});
 	
});

function confirm(form) {	
	jConfirm('', 
             'Confirm delete?', 
             function(r) {
               // If they confirmed, manually trigger a form submission
               if (r) $(form).submit();
    		  });
}

function renderPreview(id) {
	return function() {
		var md = $(id).val();
		$("#post-preview").html(markdown.core.mdToHtml(md));
	}   	
}

function onEnter(e)
{
	var keynum;
	if(window.event) // IE8 and earlier
		keynum = e.keyCode;	
	else if(e.which) // IE9/Firefox/Chrome/Opera/Safari	
		keynum = e.which;		
	if(keynum === 13) $(this).trigger("click");
}

function toggleTag(){
	$(this).toggleClass("tagoff");
	$(this).toggleClass("tagon");
	var tagText = $(this).text();
		
	if ($(this).attr("class") == "tagon") 
		$("#tag-" + tagText).val(tagText);
	else
		$("#tag-" + tagText).removeAttr("value"); 		
}

function showHelp() {
	$(".mdhelp").toggle();
}

function submitComment() {	    
    var url = context + '/comment';        
	$.post(url, 
		  {blogid:  $("#blog-id").val(),
	 	   author:  $("#author").val(),
	 	   content: $("#comment").val(),
	 	   captcha: $("#captcha").val()},
	 	   function(data){	 	          	     
    		  if("success" == data.result) {    		      		  	
    		  	window.location.reload(true);    		  	
    		  } else {
    		  	$("#captcha-link").attr("src", context + "/captcha?timestamp=" + new Date().getTime());
    		  }
  	 	   }, "json");
}