package heritage.africca.accel_connect.mapper;

import heritage.africca.accel_connect.dto.ArticleDTO;
import heritage.africca.accel_connect.entity.Article;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = UserNameMapper.class)
public interface ArticleMapper {

    @Mapping(target = "hasCoverImage", expression = "java(entity.getCoverImageData() != null)")
    @Mapping(target = "authorName", source = "author", qualifiedByName = "displayName")
    ArticleDTO toDto(Article entity);

    // createdAt/updatedAt are owned by DateAudit's lifecycle callbacks: never take them from the DTO.
    // author is stamped once by the service at creation: never take it from the DTO either.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "documentData", ignore = true)
    @Mapping(target = "documentName", ignore = true)
    @Mapping(target = "documentType", ignore = true)
    @Mapping(target = "documentSize", ignore = true)
    @Mapping(target = "coverImageData", ignore = true)
    @Mapping(target = "coverImageType", ignore = true)
    Article toEntity(ArticleDTO dto);

    // Without the `author` ignore below, every edit would blank the author out: the incoming
    // DTO never carries one.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "documentData", ignore = true)
    @Mapping(target = "documentName", ignore = true)
    @Mapping(target = "documentType", ignore = true)
    @Mapping(target = "documentSize", ignore = true)
    @Mapping(target = "coverImageData", ignore = true)
    @Mapping(target = "coverImageType", ignore = true)
    void updateEntity(ArticleDTO dto, @MappingTarget Article entity);
}
