package heritage.africca.accel_connect.mapper;

import heritage.africca.accel_connect.dto.ArticleCommentDTO;
import heritage.africca.accel_connect.entity.ArticleComment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = UserNameMapper.class)
public interface ArticleCommentMapper {

    @Mapping(target = "authorId", source = "author.id")
    @Mapping(target = "authorName", source = "author", qualifiedByName = "displayName")
    // Depends on who is asking, so the service sets it after mapping.
    @Mapping(target = "canDelete", ignore = true)
    ArticleCommentDTO toDto(ArticleComment entity);
}
