package lemana.practice.tgbot.dto.converter

import lemana.practice.tgbot.dto.UserFormsDTO
import lemana.practice.tgbot.model.UserFormsModel

object UserFormsConverter {

    fun toDTO(userFormsModel: UserFormsModel): UserFormsDTO {
        return UserFormsDTO(
            id = userFormsModel.id,
            data = userFormsModel.data
        )
    }

    fun toDTOList(userFormsModels: List<UserFormsModel>): List<UserFormsDTO> {
        return userFormsModels.map { toDTO(it) }
    }
}
