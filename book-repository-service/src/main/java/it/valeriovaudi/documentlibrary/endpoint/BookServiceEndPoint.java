package it.valeriovaudi.documentlibrary.endpoint;

import it.valeriovaudi.documentlibrary.hateoas.assembler.BookResourcesAssembler;
import it.valeriovaudi.documentlibrary.model.Book;
import it.valeriovaudi.documentlibrary.model.Page;
import it.valeriovaudi.documentlibrary.model.PdfBookMaster;
import it.valeriovaudi.documentlibrary.repository.BookRepository;
import it.valeriovaudi.documentlibrary.repository.BookRepositoryMongoImpl;
import it.valeriovaudi.documentlibrary.service.BookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.messaging.MessageChannel;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.json.Json;
import javax.json.JsonObject;
import javax.validation.Valid;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Created by Valerio on 30/04/2015.
 */
@RestController
@RequestMapping("/bookService")
public class BookServiceEndPoint {
    private static final Logger LOGGER = LoggerFactory.getLogger(BookServiceEndPoint.class);

    @Autowired
    private BookRepository bookRepository;

    private MessagingTemplate messagingTemplate;

    @Autowired
    @Qualifier("createBookByPdfInputChannel")
    private MessageChannel createBookByPdfInputChannel;

    @Autowired
    private BookResourcesAssembler bookResourcesAssembler;


    public BookServiceEndPoint() {
        this.messagingTemplate = new MessagingTemplate();
    }

    public void setBookResourcesAssembler(BookResourcesAssembler bookResourcesAssembler) {
        this.bookResourcesAssembler = bookResourcesAssembler;
    }

    public void setCreateBookByPdfInputChannel(MessageChannel createBookByPdfInputChannel) {
        this.createBookByPdfInputChannel = createBookByPdfInputChannel;
    }

    public void setMessageChannel(MessageChannel createBookByPdfInputChannel) {
        this.createBookByPdfInputChannel = createBookByPdfInputChannel;
    }
    public void setMessagingTemplate(MessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void setBookRepository(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @RequestMapping(value = "/book", method = RequestMethod.POST, consumes = "multipart/form-data")
    public ResponseEntity saveBook(@Valid PdfBookMaster pdfBookMaster, BindingResult bindingResult){
        if(!bindingResult.hasErrors()){
            String uuid = UUID.randomUUID().toString();
            Map<String,Object> message = new HashMap<>();
            message.put(BookService.MAP_MESSAGE_PAYLOAD_KEY,pdfBookMaster);
            message.put(BookService.MAP_MESSAGE_UUID_KEY,uuid);

            messagingTemplate.convertAndSend(createBookByPdfInputChannel,message);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(uuid);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @RequestMapping(value = "{bookId}/page/{pageNumber}", method = RequestMethod.GET)
    public ResponseEntity<Page> getSingePage(@PathVariable("bookId") String bookId,
                                             @PathVariable("pageNumber") int pageNumber){
        return ResponseEntity.ok(bookRepository.read(bookId, pageNumber));
    }

    @RequestMapping(value = "{bookId}/pageData/{pageNumber}", method = RequestMethod.GET)
    public ResponseEntity<byte[]> getSingePageByte(@PathVariable("bookId") String bookId,
                                                   @PathVariable("pageNumber") int pageNumber){
        return ResponseEntity.ok(bookRepository.read(bookId,pageNumber).getBytes());
    }

    @RequestMapping(value = "/book", method = RequestMethod.GET)
    public ResponseEntity getAllBooks(@RequestParam(value = "pageNumber",required = false,defaultValue = "0") Integer pageNumber,
                                      @RequestParam(value = "pageSize",required = false ,defaultValue = "0") Integer pageSize){
        return ResponseEntity.ok(bookResourcesAssembler.toResources(bookRepository.readAllBooks(pageNumber, pageSize)));
    }

    @RequestMapping(value = "/book/{bookId}", method = RequestMethod.GET)
    public ResponseEntity getBook(@PathVariable("bookId") String bookId,
                                  @RequestParam(value = "startRecord",required = false,defaultValue = "0") Integer startRecord,
                                  @RequestParam(value = "pageSize",required = false,defaultValue = "0") Integer pageSize){
        return ResponseEntity.ok(bookResourcesAssembler.toResource(bookRepository.readBook(bookId, startRecord, pageSize)));
    }


    @RequestMapping(value = "/book/{bookId}", method = RequestMethod.PUT)
    public ResponseEntity updateBookAuthor(@PathVariable("bookId") String bookId, @RequestBody String body){
        Book book = bookRepository.readBook(bookId, BookRepositoryMongoImpl.INVALID_PAGINATION_PARAMITER, BookRepositoryMongoImpl.INVALID_PAGINATION_PARAMITER);
        JsonObject readBody = Json.createReader(new StringReader(body)).readObject();
        Optional.ofNullable(readBody.getString("author")).ifPresent(book::setAuthor);
        Optional.ofNullable(readBody.getString("description")).ifPresent(book::setDescription);
        bookRepository.updateBook(book);
        return ResponseEntity.noContent().build();
    }
}
