package org.l3cache.app;

import javax.servlet.http.HttpSession;

import org.l3cache.dao.Response;
import org.l3cache.model.WritePost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.NestedServletException;

import core.utils.FileAccessException;
import core.utils.FileManager;
import core.utils.ResultCode;

@RestController
@RequestMapping("/app/posts")
public class MobileAppPostsController {
	private static final Logger log = LoggerFactory
			.getLogger(MobileAppPostsController.class);
	private static final String localhost = "http://125.209.199.221:8080";
	private static final int STARTING_COUNT = 1;
	
	@Autowired
	private PostManager postManager;

	@Autowired
	private FileManager fileManager;

	@RequestMapping("/")
	public Response userBasePostsList(@RequestParam("id") int id,
									  @RequestParam("start") int start, 
									  @RequestParam("sort") int sort) {
		log.info("[/app/posts/] request id = {}, start = {}, sort= {}", id, start, sort);
		
		Response response = new Response(ResultCode.SUCCESS);
		response.setTotal(postManager.getTotalRows());
		response.setData(postManager.getPostsLists(start, id, sort));
		return response;
	}

	@RequestMapping("/{pid}")
	public Response getPostDetail(@PathVariable("pid") long pid, Model model) {
		log.info("[/app/{}/] request", pid);
		
		Response response = new Response(ResultCode.SUCCESS);
		response.setData(postManager.getPostDetail(pid));
		return response;
	}

	@RequestMapping(value = "/new")
	public String newPost(@RequestParam("title") String title,
						  @RequestParam("shopUrl") String shopUrl,
						  @RequestParam("contents") String contents,
						  @RequestParam("image") MultipartFile image,
						  @RequestParam("price") int price, 
						  @RequestParam("id") int id,
						  HttpSession session) {

		if (!fileManager.isValidatedFile(image)) {
			return "redirect:/app/posts/error";
		}

		String uploadPath = session.getServletContext().getRealPath("/WEB-INF/postsImages");
		String fileName = fileManager.saveFile(image, uploadPath);
		WritePost post = new WritePost(title, shopUrl, contents, localhost+"/postsImages/" + fileName, price, id);
		postManager.savePost(post);
		return "redirect:/app/posts/success";

	}

	@RequestMapping(value = "/newurl")
	public Response newPostWithUrl(@RequestParam("title") String title,
							   @RequestParam("shopUrl") String shopUrl,
							   @RequestParam("contents") String contents,
							   @RequestParam("image") String imageUrl,
							   @RequestParam("price") int price, 
							   @RequestParam("id") int id) {
		
		WritePost post = new WritePost(title, shopUrl, contents, imageUrl, price, id);
		if(!post.isValidated()){
			return Response.arguemnt_error();
		}
		postManager.savePost(post);
		return Response.success();		
	}

	@RequestMapping("/success")
	public Status postSuccess() {
		return Status.success();
	}

	@RequestMapping("/error")
	public Status postError(Model model) {
		return Status.error();
	}

	@RequestMapping("/edit/{pid}")
	public String editPost(@PathVariable("pid") long pid,
						   @RequestParam("title") String title,
						   @RequestParam("shopUrl") String shopUrl,
						   @RequestParam("contents") String contents,
						   @RequestParam("image") MultipartFile image,
						   @RequestParam("price") int price, 
						   @RequestParam("id") int id,
						   HttpSession session) {

		if (!fileManager.isValidatedFile(image) || !postManager.isExistentPost(pid)) {
			return "redirect:/app/posts/error";
		}

		String uploadPath = session.getServletContext().getRealPath("/WEB-INF/postsImages/");
		String beforeFile = postManager.getPostImageFilePath(pid);
		String fileName = fileManager.saveAndRemoveFile(image, uploadPath, beforeFile);

		WritePost post = new WritePost(title, shopUrl, contents, localhost+"/postsImages/" + fileName, price, id);
		postManager.updateWithImage(post);
		return "redirect:/app/posts/success";
	}
	
	@RequestMapping(value = "/editurl/{pid}")
	public Response editPostWithUrl(@RequestParam("title") String title,
							  		@RequestParam("shopUrl") String shopUrl,
							  		@RequestParam("contents") String contents,
							  		@RequestParam("image") String imageUrl,
							  		@RequestParam("price") int price, 
							  		@RequestParam("id") int id) {
		
		WritePost post = new WritePost(title, shopUrl, contents, imageUrl, price, id);
		if(!post.isValidated()){
			return Response.arguemnt_error();
		}
		postManager.savePost(post);
		return Response.success();		
	}

	@RequestMapping(value = "/delete/{pid}", method = { RequestMethod.DELETE, RequestMethod.GET })
	public Response deletePost(@PathVariable("pid") long pid,
					       @RequestParam("uid") int uid) {
		postManager.deletePost(pid, uid);
		return Response.success();
	}

	@RequestMapping(value = "/like", method = { RequestMethod.POST, RequestMethod.GET })
	public Response likePost(@RequestParam("pid") long pid,
							 @RequestParam("uid") int uid, Model model) {
		postManager.likePost(pid, uid);
		return Response.success();
	}

	@RequestMapping(value = "/like", method = { RequestMethod.DELETE, RequestMethod.GET })
	public Response unlikePost(@RequestParam("pid") long pid,
			@RequestParam("uid") int uid, Model model) {
		postManager.unlikePost(pid, uid);
		return Response.success();
	}

	@RequestMapping(value = "/{pid}/read", method = { RequestMethod.POST, RequestMethod.GET })
	public Response readPost(@PathVariable("pid") long pid, Model model) {
		postManager.readPost(pid);
		return Response.success();
	}

	@RequestMapping(value="/{uid}/likes")
	public Response getMyLikes(@PathVariable("uid") int uid,
							   @RequestParam(value = "start") int start) {
		if(uid<STARTING_COUNT && start<STARTING_COUNT){
			return Response.error();
		}
		
		Response response = Response.success();
		response.setTotal(postManager.getUserLikesCount(uid));
		response.setData(postManager.getUserLikesList(uid, start));
		return response;
	}
	
	@RequestMapping(value="/{uid}/posts")
	public Response getMySnap(@PathVariable("uid") int uid,
							  @RequestParam(value = "start") int start) {
		if(uid<STARTING_COUNT && start<STARTING_COUNT){
			return Response.error();
		}
		
		Response response = Response.success();
		response.setTotal(postManager.getUserPostsCount(uid));
		response.setData(postManager.getUserPostsList(uid, start));
		return response;
	}
	
	
	@ExceptionHandler(value={ NestedServletException.class, FileAccessException.class, IllegalArgumentException.class})
	public ModelAndView exceptionHandler(Exception e) {
		log.error("NestedServletException : ", e.toString());
		return new ModelAndView("NestedServletException").addObject("status",ResultCode.ERROR);
	}

}
