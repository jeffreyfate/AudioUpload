package com.jeffthefate.dmb.audio;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class AudioUpload {
	
	private static File lastDir = null;
	private static ArrayList<String> chosenFiles = new ArrayList<String>();

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		startChoosing(null);
		String name = null;
		String objectId = null;
		boolean passed = true;
		for (String file : chosenFiles) {
			//deleteAudio("80eda357-6aaf-42bb-a088-83aa414ed14e-aftw.mp3");
			name = getNameFromFile(file);
			objectId = checkUpload(name);
			if (objectId == null)
				passed = associateUpload(getAssociateJsonString(name, uploadAudio(file)));
			else
				updateAudio(objectId, getAssociateJsonString(name, uploadAudio(file)));
			if (!passed) {
				JOptionPane.showMessageDialog(null, "Upload failed!", "", JOptionPane.ERROR_MESSAGE);
				return;
			}
		}
		JOptionPane.showMessageDialog(null, "Upload complete", "", JOptionPane.INFORMATION_MESSAGE);
	}
	
	private static void startChoosing(File directory) {
		chosenFiles = filterFiles(showFileChooser(directory), ".mp3");
		for (String file : chosenFiles) {
			System.out.println(file);
		}
		if (chosenFiles.isEmpty()) {
			switch(JOptionPane.showOptionDialog(null, "No files found!", "",
					JOptionPane.OK_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE,
					null, null, null)) {
			case JOptionPane.OK_OPTION:
				startChoosing(lastDir);
				break;
			}
		}
	}
	
	private static String uploadAudio(String filename) {
		final HttpParams httpParams = new BasicHttpParams();
	    HttpConnectionParams.setConnectionTimeout(httpParams, 30000);
		DefaultHttpClient httpclient = new DefaultHttpClient(httpParams);
		HttpResponse response = null;
		HttpPost httpPost = new HttpPost("https://api.parse.com/1/files/" + filename);
		String uploadString = null;
		String parseFilename = null;
		String filePath = lastDir.getPath().concat(File.separator).concat(filename);
    	File file = new File(filePath);
		MultipartEntity entity = new MultipartEntity();
		FileBody fileBody = new FileBody(file);
		entity.addPart("file", fileBody);
		httpPost.setEntity(entity);
        httpPost.addHeader("X-Parse-Application-Id", "ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R");
        httpPost.addHeader("X-Parse-REST-API-Key", "1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        httpPost.addHeader("Content-Type", "audio/mpeg");
        try {
			response = httpclient.execute(httpPost);
			uploadString = EntityUtils.toString(response.getEntity());
			// {"url":"http://files.parse.com/be170a0a-047c-41d0-8474-c1b80f509371/c2d8e768-8c96-481f-ab0b-3c67dfade8e3-aftw.mp3","name":"c2d8e768-8c96-481f-ab0b-3c67dfade8e3-aftw.mp3"}
			System.out.println(uploadString);
		} catch (IOException e) {
			System.out.println("Failed to upload audio: " + filename);
			e.printStackTrace();
		}
        if (uploadString != null)
        	parseFilename = getNameFromUpload(uploadString);
        return parseFilename;
	}
	
	private static String getAssociateJsonString(String name, String uploadFilename) {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        ObjectNode rootNode = factory.objectNode();
        ObjectNode fileNode = factory.objectNode();
        fileNode.put("__type", "File");
        fileNode.put("name", uploadFilename);
        rootNode.put("name", name);
        rootNode.put("file", fileNode);
        return rootNode.toString();
    }
	
	private static String getCheckUploadJsonString(String name) {
		JsonNodeFactory factory = JsonNodeFactory.instance;
		ObjectNode rootNode = factory.objectNode();
		rootNode.put("name", name);
		return rootNode.toString();
	}
	
	private static String checkUpload(String name) {
		DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpResponse response = null;
        HttpEntity entity = null;
        String responseString = null;
        String url = "https://api.parse.com/1/classes/Audio?";
        try {
            url += URLEncoder.encode("where={\"name\":\"" + name + "\"}", "US-ASCII");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader("X-Parse-Application-Id", "ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R");
        httpGet.addHeader("X-Parse-REST-API-Key", "1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        try {
            response = httpclient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() != 200) {
                System.out.println("GET of " + name + " failed!");
                return null;
            }
            entity = response.getEntity();
            if (entity != null)
                 responseString = EntityUtils.toString(response.getEntity());
        } catch (ClientProtocolException e1) {
            System.out.println("Failed to connect to " +
                    httpGet.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            System.out.println("Failed to get audio entry from " +
                    httpGet.getURI().toASCIIString());
            e1.printStackTrace();
        }
        if (responseString == null) {
            System.out.println("Fetch setlist from Parse failed!");
            System.out.println(name);
        }
        return hasResult(responseString);
	}
	
	private static boolean associateUpload(String json) {
		DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpResponse response = null;
        HttpPost httpPost = new HttpPost("https://api.parse.com/1/classes/Audio");
        httpPost.addHeader("X-Parse-Application-Id", "ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R");
        httpPost.addHeader("X-Parse-REST-API-Key", "1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        httpPost.addHeader("Content-Type", "application/json; charset=utf-8");
        try {
            StringEntity reqEntity = new StringEntity(json, "UTF-8");
            httpPost.setEntity(reqEntity);
            response = httpclient.execute(httpPost);
            if (response.getStatusLine().getStatusCode() != 201) {
                System.out.println("POST of audio failed!");
                System.out.println(json);
                return false;
            } 
        } catch (UnsupportedEncodingException e) {
            System.out.println("Failed to create entity from " + json);
            e.printStackTrace();
            return false;
        } catch (ClientProtocolException e1) {
            System.out.println("Failed to connect to " +
                    httpPost.getURI().toASCIIString());
            e1.printStackTrace();
            return false;
        } catch (IOException e1) {
            System.out.println("Failed to get setlist from " +
                    httpPost.getURI().toASCIIString());
            e1.printStackTrace();
            return false;
        }
        return true;
	}
	
	private static void updateAudio(String objectId, String json) {
		DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpResponse response = null;
		HttpPut httpPut = new HttpPut("https://api.parse.com/1/classes/Audio/" + objectId);
        httpPut.addHeader("X-Parse-Application-Id", "ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R");
        httpPut.addHeader("X-Parse-REST-API-Key", "1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        httpPut.addHeader("Content-Type", "application/json; charset=utf-8");
        try {
            StringEntity reqEntity = new StringEntity(json, "UTF-8");
            httpPut.setEntity(reqEntity);
            response = httpclient.execute(httpPut);
            if (response.getStatusLine().getStatusCode() != 200) {
                System.out.println("PUT to " + objectId + " failed!");
                System.out.println(json);
            }  
        } catch (UnsupportedEncodingException e) {
            System.out.println("Failed to create entity from " + json);
            e.printStackTrace();
        } catch (ClientProtocolException e1) {
            System.out.println("Failed to connect to " +
                    httpPut.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            System.out.println("Failed to get setlist from " +
                    httpPut.getURI().toASCIIString());
            e1.printStackTrace();
        }
	}
	
	private static boolean deleteAudio(String filename) {
		// c2d8e768-8c96-481f-ab0b-3c67dfade8e3-aftw.mp3
		DefaultHttpClient httpclient = new DefaultHttpClient();
		HttpResponse response = null;
		HttpDelete httpDelete = new HttpDelete("https://api.parse.com/1/files/" + filename);
		httpDelete.addHeader("X-Parse-Application-Id", "ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R");
		httpDelete.addHeader("X-Parse-Master-Key", "QirtSimQTDJhPsCsIdGbEz9ymw5gclXhugs0l6ZD");
        try {
			response = httpclient.execute(httpDelete);
			System.out.println(EntityUtils.toString(response.getEntity()));
		} catch (IOException e) {
			System.out.println("Failed to delete audio: " + filename);
			e.printStackTrace();
		}
        return true;
	}
	
	private static String getNameFromUpload(String uploadString) {
        JsonFactory f = new JsonFactory();
        JsonParser jp;
        String fieldname;
        String name = null;
        try {
            jp = f.createJsonParser(uploadString);
            while (jp.nextToken() != null) {
                fieldname = jp.getCurrentName();
                if ("name".equals(fieldname)) {
                	jp.nextToken();
                    name = jp.getText();
                    jp.close();
                    return name;
                }
            }
            jp.close(); // ensure resources get cleaned up timely and properly
        } catch (JsonParseException e) {
            System.out.println("Failed to parse " + uploadString);
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Failed to parse " + uploadString);
            e.printStackTrace();
        }
        return name;
    }
	
	private static String hasResult(String getString) {
        JsonFactory f = new JsonFactory();
        JsonParser jp;
        String fieldname;
        String result = null;
        try {
        	System.out.println(getString);
        	jp = f.createJsonParser(getString);
            jp.nextToken();
            jp.nextToken();
            fieldname = jp.getCurrentName();
            if ("results".equals(fieldname)) { // contains an object
                jp.nextToken();
                while (jp.nextToken() != null) {
                    jp.nextToken();
                    fieldname = jp.getCurrentName();
                    if ("objectId".equals(fieldname)) {
                    	//jp.nextToken();
                    	result = jp.getText();
                    	jp.close();
                    	return result;
                    }
                }
            }
            jp.close(); // ensure resources get cleaned up timely and properly
        } catch (JsonParseException e) {
            System.out.println("Failed to parse " + getString);
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Failed to parse " + getString);
            e.printStackTrace();
        }
        return result;
    }
	
	private static String getNameFromFile(String filename) {
		return filename.substring(0, filename.lastIndexOf("."));
	}
	
	private static JFileChooser createFileChooser(File file) {
		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
	    if (file != null)
	    	chooser.setSelectedFile(file);
	    return chooser;
	}
	
	private static String[] processChooser(JFileChooser chooser) {
		int choice = chooser.showOpenDialog(null);
		switch(choice) {
		case JFileChooser.APPROVE_OPTION:
			lastDir = new File(chooser.getSelectedFile().getPath());
			switch(JOptionPane.showOptionDialog(null,
					"Are you sure?\n" + chooser.getSelectedFile().getPath(), "",
					JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
					null, null, null)) {
			case JOptionPane.YES_OPTION:
				return getFileNamesFromPath(chooser.getSelectedFile().getPath());
			case JOptionPane.NO_OPTION:
				return showFileChooser(lastDir);
			case JOptionPane.CLOSED_OPTION:
				break;
			}
			break;
		default:
			break;
		}
		return null;
	}
	
	private static String[] showFileChooser(File file) {
		return processChooser(createFileChooser(file));
	}
	
	private static String[] getFileNamesFromPath(String path) {
		File dir = new File(path);
        String[] files = dir.list();
        return files;
	}
	
	private static ArrayList<String> filterFiles(String[] files, String extension) {
		ArrayList<String> filtered = new ArrayList<String>();
		if (files != null) {
			for (int i = 0; i < files.length; i++) {
				if (files[i].endsWith(extension))
					filtered.add(files[i]);
			}
		}
		return filtered;
	}

}
