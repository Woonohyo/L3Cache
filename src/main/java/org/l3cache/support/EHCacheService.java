package org.l3cache.support;

import java.util.Map;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.l3cache.dto.Response;
import org.l3cache.dto.SearchResult;
import org.l3cache.dto.ShopItems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import core.utils.ResultCode;

@Service
public class EHCacheService {
	private static final Logger LOG = LoggerFactory.getLogger(EHCacheService.class);

	private static final int DISPLAY_COUNT = 20;
	
	@Autowired
	SearchHelper searchHelper;
	
	@Autowired
	Cache cache;
	
	public EHCacheService() {
	}

	public Response getResponse(Map<String, Object> params) {
		String key = makeKey((String) params.get("query"), (int)params.get("start"), (String)params.get("sort"));
		Element ele = cache.get(key);
		
		if(ele != null){
			LOG.debug("return Cashed Api ( key ={})",key);
			return (Response) ele.getObjectValue();
		}else{
			LOG.debug("return non Cashed Api ( key ={})",key);
			return naverApiToResponse(params);
		}
	}
	
	private Response naverApiToResponse(Map<String, Object> params) {
		int start = (int)params.get("start");
		ShopItems si = searchHelper.searchNaverApi(params);
		
		int total = si.getChannel().getTotal();
		
		if(total==0){
			return Response.resultZero();
		}else if(total<=20){
			Response response = new Response(ResultCode.SUCCESS);
			SearchResult result = new SearchResult(start, total, si.getChannel().getItem().subList(0, total));
			response.setData(result);
			
			apiCache(si, params, start, total);
			return response;
		}else{
			Response response = new Response(ResultCode.SUCCESS);
			SearchResult result = new SearchResult(start, total, si.getChannel().getItem().subList(0, DISPLAY_COUNT));
			response.setData(result);
			apiCache(si, params, start, total);
			return response;
		}
		
	}

	@Async
	private void apiCache(ShopItems si, Map<String, Object> params, int start, int end) {
		int totalPage = 0;
		
		if(end>100){
			totalPage = 5;
		}else{
			totalPage = end / DISPLAY_COUNT;
		}
		
		for(int i=0; i<totalPage; i++){
			int pageIdx = i*DISPLAY_COUNT;
			int realStart = start + i;
			int total = si.getChannel().getTotal();
			
			Response response = new Response(ResultCode.SUCCESS);
			SearchResult result = new SearchResult(realStart, total, si.getChannel().getItem().subList(pageIdx, pageIdx + DISPLAY_COUNT));
			response.setData(result);
			
			String key = makeKey((String) params.get("query"), realStart, (String)params.get("sort"));
			
			Element element = new Element(key, response);
			cache.put(element);
			LOG.debug("Api Cashe ( key ={})",key);
		}
	}

	private String makeKey(String param, int start, String sort) {
		StringBuffer sb = new StringBuffer();
		sb.append(param);
		sb.append("_");
		sb.append(start);
		sb.append("_");
		sb.append(sort);
		return sb.toString();
	}

}
