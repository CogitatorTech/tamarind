package io.github.cogitatortech.tamarind.rest.v1;

import io.github.cogitatortech.tamarind.engine.QueryEngine;
import io.github.cogitatortech.tamarind.rest.v1.dto.ApiResponse;
import io.github.cogitatortech.tamarind.security.UserService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

/** REST endpoint for data source management including file uploads and S3 table registration. */
@Path("/api/v1/datasources")
@Produces(MediaType.APPLICATION_JSON)
public class DataSourceResource {

  private static final Logger LOGGER = LogManager.getLogger(DataSourceResource.class);
  private static final String UPLOAD_DIR = "./data/uploads";
  private static final long MAX_FILE_SIZE = 100 * 1024 * 1024; // 100MB

  @Inject QueryEngine engine;
  @Inject UserService userService;

  @POST
  @Path("/upload")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public Response uploadFile(@HeaderParam("Authorization") String authHeader, FileUploadForm form) {
    try {
      // Validate authentication
      if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        return Response.status(Response.Status.UNAUTHORIZED)
            .entity(ApiResponse.error("UNAUTHORIZED", "Invalid or missing token"))
            .build();
      }

      String token = authHeader.substring(7);
      if (!userService.validateToken(token)) {
        return Response.status(Response.Status.UNAUTHORIZED)
            .entity(ApiResponse.error("UNAUTHORIZED", "Invalid token"))
            .build();
      }

      // Validate file
      if (form.file == null) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(ApiResponse.error("INVALID_REQUEST", "No file provided"))
            .build();
      }

      String fileName = form.file.fileName();
      if (fileName == null || fileName.isEmpty()) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(ApiResponse.error("INVALID_REQUEST", "Invalid file name"))
            .build();
      }

      // Validate file type
      String extension = getFileExtension(fileName).toLowerCase();
      if (!isValidFileType(extension)) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(
                ApiResponse.error(
                    "INVALID_FILE_TYPE", "Unsupported file type. Supported: parquet, csv, json"))
            .build();
      }

      // Check file size
      File uploadedFile = form.file.uploadedFile().toFile();
      if (uploadedFile.length() > MAX_FILE_SIZE) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(ApiResponse.error("FILE_TOO_LARGE", "File size exceeds maximum limit of 100MB"))
            .build();
      }

      // Create upload directory if it doesn't exist
      Files.createDirectories(Paths.get(UPLOAD_DIR));

      // Save file with unique name
      String uniqueFileName = System.currentTimeMillis() + "_" + fileName;
      String filePath = UPLOAD_DIR + "/" + uniqueFileName;
      Files.copy(uploadedFile.toPath(), Paths.get(filePath));

      // Create view from file if table name provided
      String viewName = form.tableName;
      if (viewName != null && !viewName.trim().isEmpty()) {
        engine.createViewFromFile(viewName, filePath);
        LOGGER.info("Created view '{}' from uploaded file: {}", viewName, fileName);
      }

      Map<String, Object> data = new HashMap<>();
      data.put("fileName", uniqueFileName);
      data.put("originalName", fileName);
      data.put("filePath", filePath);
      data.put("size", uploadedFile.length());
      data.put("type", extension);
      if (viewName != null && !viewName.trim().isEmpty()) {
        data.put("tableName", viewName);
      }

      LOGGER.info("File uploaded successfully: {} ({} bytes)", fileName, uploadedFile.length());
      return Response.ok(ApiResponse.success(data)).build();

    } catch (Exception e) {
      LOGGER.error("Error uploading file", e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(ApiResponse.error("INTERNAL_ERROR", "Failed to upload file", e.getMessage()))
          .build();
    }
  }

  @POST
  @Path("/s3/register")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response registerS3Table(
      @HeaderParam("Authorization") String authHeader, S3TableRequest request) {
    try {
      // Validate authentication
      if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        return Response.status(Response.Status.UNAUTHORIZED)
            .entity(ApiResponse.error("UNAUTHORIZED", "Invalid or missing token"))
            .build();
      }

      String token = authHeader.substring(7);
      if (!userService.validateToken(token)) {
        return Response.status(Response.Status.UNAUTHORIZED)
            .entity(ApiResponse.error("UNAUTHORIZED", "Invalid token"))
            .build();
      }

      // Validate request
      if (request.tableName == null || request.tableName.trim().isEmpty()) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(ApiResponse.error("INVALID_REQUEST", "Table name is required"))
            .build();
      }

      if (request.s3Path == null || request.s3Path.trim().isEmpty()) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(ApiResponse.error("INVALID_REQUEST", "S3 path is required"))
            .build();
      }

      // Validate S3 path format
      if (!request.s3Path.startsWith("s3://")) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(ApiResponse.error("INVALID_REQUEST", "S3 path must start with s3://"))
            .build();
      }

      // Create view from S3 path
      engine.createViewFromFile(request.tableName, request.s3Path);

      Map<String, Object> data = new HashMap<>();
      data.put("tableName", request.tableName);
      data.put("s3Path", request.s3Path);
      data.put("status", "registered");

      LOGGER.info("S3 table registered: {} -> {}", request.tableName, request.s3Path);
      return Response.ok(ApiResponse.success(data)).build();

    } catch (Exception e) {
      LOGGER.error("Error registering S3 table", e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(
              ApiResponse.error("INTERNAL_ERROR", "Failed to register S3 table", e.getMessage()))
          .build();
    }
  }

  @GET
  @Path("/tables")
  public Response listTables(@HeaderParam("Authorization") String authHeader) {
    try {
      // Validate authentication
      if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        return Response.status(Response.Status.UNAUTHORIZED)
            .entity(ApiResponse.error("UNAUTHORIZED", "Invalid or missing token"))
            .build();
      }

      String token = authHeader.substring(7);
      if (!userService.validateToken(token)) {
        return Response.status(Response.Status.UNAUTHORIZED)
            .entity(ApiResponse.error("UNAUTHORIZED", "Invalid token"))
            .build();
      }

      var tables = engine.getMetadata().getTables();
      return Response.ok(ApiResponse.success(tables)).build();

    } catch (Exception e) {
      LOGGER.error("Error listing tables", e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(ApiResponse.error("INTERNAL_ERROR", "Failed to list tables"))
          .build();
    }
  }

  @GET
  @Path("/columns/{table}")
  public Response listColumns(
      @HeaderParam("Authorization") String authHeader,
      @PathParam("table") String table,
      @QueryParam("includeTypes") @DefaultValue("false") boolean includeTypes) {
    try {
      if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        return Response.status(Response.Status.UNAUTHORIZED)
            .entity(ApiResponse.error("UNAUTHORIZED", "Invalid or missing token"))
            .build();
      }
      String token = authHeader.substring(7);
      if (!userService.validateToken(token)) {
        return Response.status(Response.Status.UNAUTHORIZED)
            .entity(ApiResponse.error("UNAUTHORIZED", "Invalid token"))
            .build();
      }

      if (table == null || table.isBlank()) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(ApiResponse.error("INVALID_REQUEST", "Table name is required"))
            .build();
      }

      if (!includeTypes) {
        var cols = engine.getMetadata().getColumns(table);
        return Response.ok(ApiResponse.success(cols)).build();
      }

      // Typed metadata using direct JDBC metadata (no DESCRIBE fallback)
      var typed = engine.getMetadata().getColumnsWithTypes(table);
      var out = new java.util.ArrayList<java.util.Map<String, Object>>();
      for (var c : typed) {
        var m = new java.util.HashMap<String, Object>();
        m.put("name", c.getName());
        if (c.getType() != null) m.put("type", c.getType());
        out.add(m);
      }
      return Response.ok(ApiResponse.success(out)).build();
    } catch (Exception e) {
      LOGGER.error("Error listing columns for table {}", table, e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(ApiResponse.error("INTERNAL_ERROR", "Failed to list columns"))
          .build();
    }
  }

  private String getFileExtension(String fileName) {
    int lastDot = fileName.lastIndexOf('.');
    return lastDot > 0 ? fileName.substring(lastDot + 1) : "";
  }

  private boolean isValidFileType(String extension) {
    return extension.equals("parquet") || extension.equals("csv") || extension.equals("json");
  }

  public static class FileUploadForm {
    @RestForm("file")
    public FileUpload file;

    @RestForm("tableName")
    public String tableName;
  }

  public static class S3TableRequest {
    public String tableName;
    public String s3Path;
  }
}
